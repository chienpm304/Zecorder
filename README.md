# Zecorder
```
 Android screen recording and streaming application
```
## Features
### 1. SCREEN RECORDING
- Record screen with custom settings and advanced media specifications​

- Controller head while recording​

- Time-countdown before start recording​

- Funny camera preview​

- Toggle/switch camera immediately​

- Draw decorators (logo, watermark…) onto video​

### 2. SCREEN STREAMING​
- Realtime streaming screen to RTMP sever​

- Resumable streaming supports​

- Camera preview ​

- Test connection before streaming​

- Update streaming status onto app view​

###  3. CAMERA PREVIEW
- Realtime camera preview while recording or streaming​

- Toggle camera view (On/Off)​

- Switch camera (Back/Face)​

- Set camera size (Small/Medium/Big)​

- Choose camera position

### 4. VIDEO MANAGER- List videos recorded by our app​
- Rename, delete videos​

- Extract video details​

- Auto update videos list​

- Detect and remove invalid videos​

### 5. SYNCHRONIZATION
- Synchronize local video to Google Drive Storage​

- Authentication with Google Account​

- Run in background service and auto-close when sync completed​

- Allow to choose specific videos to sync​

- Sync status for each videos​

--------------------------
## ARCHITECTURE
### 1. Big picture
![alt text](https://user-images.githubusercontent.com/37016896/64228983-b4ba9b00-cf12-11e9-90e1-a4c99374a6dd.jpg)

### 2. Streaming picture
![alt text](https://user-images.githubusercontent.com/37016896/64229091-06fbbc00-cf13-11e9-8a2b-a94074b1134d.jpg)


---------------------------
## STREAMING USEAGE
### 1. Build RTMP-Server

<strong>Step 1:</strong> get SRS 

<pre>
git clone https://github.com/ossrs/srs &&
cd srs/trunk
</pre>

<strong>Step 2:</strong> build SRS,
<strong>Requires Centos6.x/Ubuntu 32/64bits.</strong>

<pre>
./configure && make
</pre>

<strong>Step 3:</strong> start SRS 

<pre>
./objs/srs -c conf/srs.conf
</pre>


### 2. Application side
<strong>Step 1:</strong> get app source 

<pre>
git clone https://github.com/1612052/Zecorder.git
</pre>

<strong>Step 2:</strong> Build and run application

<strong>Step 3:</strong>
- Swipe to Livestream layout
- Enter the server ip
- Tap "Test" button
- Tap "Connect" button
- Enjoy!!!

### * Video guide/demo:

- <strong>Server: </strong> [server side](https://studenthcmusedu-my.sharepoint.com/:v:/g/personal/1612052_student_hcmus_edu_vn/Ec0BYU6ZfVpLluCufBeL8lcBnr9yVoRiXp-EKN6Z-UAdmg?e=rmi8TO)

- <strong>App : </strong> [client side](https://studenthcmusedu-my.sharepoint.com/:v:/g/personal/1612052_student_hcmus_edu_vn/EUvwFO83bc9Dp7TRqRS3glwBWG7AC_dvyB3VcS8zEshL5g?e=6JGrNw)





