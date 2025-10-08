        if (fluffyConfig.transcribeDisableListenSignal && messageObject.preventTranscribeMarkAsRead) {
            return;
        }
        if (fluffyConfig.transcribeDisableListenSignal && MessageObject.shouldPreventTranscribeMarkAsRead(messageObject)) {
