<p align="center"><img width="30%" vspace="20" src="https://github.com/rotorlab/database-kotlin/raw/develop/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png"></p>

# Rotor Database

Work with synchronized Java objects stored as JSON objects. 

### Requirements
**1º redis-server:** Amazing Pub/Sub engine for real-time changes. Simply install and start it.

**2º rotor-server:** It will be our server cluster for storing json objects. Server cluster is run with **node** framework.

Check out [rotor-server repo](https://github.com/rotorlab/database-kotlin) for more information.

### Usage
- Import library:

```groovy
android {
 
    defaultConfig {
        multiDexEnabled true
    }
    
}
 
dependencies {
    implementation 'com.rotor:core:0.0.5'
}
```
- Initialize library on Java:
```java
// redis url starts with redis://, port is not included
Rotor.initialize(getApplicationContext(), "http://10.0.2.2:1507/", "redis://10.0.2.2", new StatusListener() {
 
    @Override
    public void connected() {
        /* fired only when initialized method is called and library is connected to redis */
    }
    
    @Override
    public void reconnecting() {
        /* library is trying to connect to redis */
    }
  
});
```
On Kotlin:
```kotlin
Rotor.initialize(applicationContext, "http://10.0.2.2:1507/", "redis://10.0.2.2", object: StatusListener {
 
    override fun connected() {
        
    }
 
    override fun reconnecting() {
        
    }
 
})
```
Debug:
```kotlin
// debug logs
Rotor.setDebug(true);
```
Background updates (not optional)
------------------
Rotor Database library works in background in order to receive updates when application is on background or foreground. You must add RotorService to your `AndroidManifest.xml` file:
```xml
<application>
 
    <service
        android:name="com.rotor.core.RotorService"
        android:enabled="true"
        android:exported="true" />
 
</application>
```
This service is controlled when the application is present and must be `bind` or `unbind`. Add in activities:
```java
@Override
protected void onResume() {
    super.onResume();
    Rotor.onResume();
}
 
@Override
protected void onPause() {
    Rotor.onPause();
    super.onPause();
}
```

License
-------
    Copyright 2018 Efraín Espada

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
