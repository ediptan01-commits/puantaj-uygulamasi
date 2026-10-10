import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
appId: 'com.ediptan.kurumsalpuantaj.ios',
appName: 'Kurumsal Puantaj',
webDir: 'www',
bundledWebRuntime: false,
ios: {
contentInset: 'automatic',
backgroundColor: '#f2f5fb'
}
};

export default config;
