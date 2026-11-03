import http from 'node:http';
import {GoogleAuth} from 'google-auth-library';
import {verifyPurchase} from './core.mjs';
const auth=new GoogleAuth({scopes:['https://www.googleapis.com/auth/androidpublisher']});
const limit=new Map();
setInterval(()=>limit.clear(),60000).unref();
const server=http.createServer(async(req,res)=>{
    res.setHeader('Content-Type','application/json');res.setHeader('Cache-Control','no-store');
    const reply=(status,value)=>{res.writeHead(status);res.end(JSON.stringify(value));};
    if(req.method!=='POST'||req.url!=='/verify'){reply(404,{error:'not_found'});return;}
    const ip=req.socket.remoteAddress;
    // Baseline in-process limit; add a real gateway limit before exposing production traffic.
    if(limit.size>10000||((limit.get(ip)||0)>=60)){reply(429,{error:'rate_limit'});return;}
    limit.set(ip,(limit.get(ip)||0)+1);
    try{
        let body='';for await(const chunk of req){body+=chunk.toString('utf8');if(Buffer.byteLength(body)>16384){reply(413,{error:'request_too_large'});return;}}
        let request;try{request=JSON.parse(body);}catch{reply(400,{error:'invalid_json'});return;}
        const client=await auth.getClient();
        const result=await verifyPurchase(request,async(path)=>{
            const response=await client.request({url:'https://androidpublisher.googleapis.com/androidpublisher/v3/'+path,timeout:10000});
            return response.data;
        });
        reply(200,result);
    }catch(error){
        // Never log request bodies, purchase tokens or Google responses.
        const status=error.status===400?400:503;
        reply(status,{error:status===400?'invalid_request':'verification_unavailable'});
    }
});
server.requestTimeout=15000;server.headersTimeout=10000;
server.listen(Number(process.env.PORT)||8080,'0.0.0.0');
