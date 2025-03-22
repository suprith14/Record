import React, { useState, useEffect } from 'react';
import { View, Text, Button, PermissionsAndroid, Alert } from 'react-native';
import RNMail from 'react-native-mail';
import { NativeModules, NativeEventEmitter } from 'react-native';


console.log('Available Native Modules:', NativeModules);
const { CallRecorderModule } = NativeModules;

const App = () => {
  const [recording, setRecording] = useState(false);

  useEffect(() => {
    const eventEmitter = new NativeEventEmitter(CallRecorderModule);
    eventEmitter.addListener('onCallRecorded', (filePath) => {
      sendEmail(filePath);
    });
  }, []);

  const requestPermissions = async () => {
    try {
      const granted = await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
        PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
      ]);

      if (
        granted['android.permission.RECORD_AUDIO'] === 'granted' &&
        granted['android.permission.READ_PHONE_STATE'] === 'granted' &&
        granted['android.permission.WRITE_EXTERNAL_STORAGE'] === 'granted'
      ) {
        Alert.alert('Permissions Granted');
      } else {
        Alert.alert('Permissions Denied');
      }
    } catch (err) {
      console.warn(err);
    }
  };

  const startRecording = async () => {
    try {
      CallRecorderModule.startRecording();
      setRecording(true);
      Alert.alert('Recording Started');
    } catch (error) {
      console.error(error);
    }
  };

  const stopRecording = async () => {
    try {
      CallRecorderModule.stopRecording();
      setRecording(false);
      Alert.alert('Recording Stopped');
    } catch (error) {
      console.error(error);
    }
  };

  const sendEmail = (filePath) => {
    RNMail.send({
      subject: 'Recorded Call File',
      recipients: ['your-email@example.com'],
      body: 'Find the attached call recording.',
      attachment: {
        path: filePath,
        type: 'audio/mpeg',
        name: 'call_record.mp3',
      },
    })
      .then(() => Alert.alert('Email Sent'))
      .catch((error) => console.error(error));
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Text>Call Recorder App</Text>
      <Button title="Request Permissions" onPress={requestPermissions} />
      <Button title="Start Recording" onPress={startRecording} disabled={recording} />
      <Button title="Stop Recording" onPress={stopRecording} disabled={!recording} />
    </View>
  );
};

export default App;
