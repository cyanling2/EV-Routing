# EV-Routing
EV-Routing is an Android navigation app for electric vehicles which aims to solve EV users' range anxieties.
## Description
One major reason that prevents electric vehicles, one of the ways to move towards a more sustainable environment, to be widely adopted is the range anxiety. According to studies by visualcapitalist and EVBOX, the average EV range is 211 miles, which is only half of what a gas car can cover. While there are about 3,000,000 electric cars in the United States, there are only 20,431 level 3 chargers (level 3 charger, AKA DC fast & supercharger, provides 400-900 volts and charges at 3-20 miles per minute). Sources of the above data are listed in `Sources`.People are generally worried that their electric vehicles may run out of battery in the middle of a long trip while they can not find a fast charger nearby. The EV-Routing app aims to solve this problem by providing a route planning service that takes charging problems into consideration for EV users. The app should be able to plan a route that finds compatible charging stations as intemediate points efficiently based on the destination and vehicle information provided by the user. Tesla has a similar app embedded in its built-in navigation system which serves tesla users only. This app aims to provide a more general solution that help users of other brands as well.
## Installation
### set up Android Studio
1. Download and install [Android Studio](https://developer.android.com/studio).
2. Clone the repo to your `AndroidStudioProjects` folder. For Windows users, by default it should be at \
`C:\Users\<your_user_name>\AndroidStudioProjects`.
3. Open the code repository from Android Studio. Include your sdk direction in your local.properties file `sdk.dir={your direction}`.
You can do so from UI as well:
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/image3.png)
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/image7.png)
4. Create your emulator if you don’t have a real device (else, please refer to the next section)
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/image1.png)
Make sure that Phone is selected for Category and most importantly, you choose a device that has a Play Store icon. After choosing the hardware, you will be prompted to choose a system image and the recommended highlighted one should be good. After choosing a system image, hit Finish and there should be a device in the Device Manager. Now, when the Run button is clicked, it will build and run the app on the emulator device that was created.
### set up Google API key
1. Set up your [Google Map Platform](https://developers.google.com/maps) and create the project.
2. Create API_Key1. Set the API restrictions to be `MAPS SDK for Android` and `Places API`. open your Android Studio project, in `local.properties` under `Gradle Scripts`, add your key.
```
MAPS_API_KEY= = <Your_API_Key1>
```
This will be automatically compiled and transferred to BuildConfig.java once you run “build”.
3. Create API_Key2. Set the API restrictions to be `Directions API`. open your Android Studio project, in `app.java.com.cmu.evplan.com.RoutingFragment.processEVJson`, change the api_key to be your key.
```
val api_key = <Your_API_Key2>
```
The Directions API is probably not free. You may need to link your project to a billing account at this step. \
4. For your convenience, you can set both keys as unrestricted if you want to make use of other fabulous APIs that Google provides, but make sure you keep the keys to your team and monitor your billing account regularly. \
5. Make sure you synchronize your settings from time to time.
![alt text](https://github.com/cyanling2/EV-Routing/blob/main/images/image4.png)
### Deploy on Android Device
1. [Turn on development mode in your android device and enable USB-debugging](https://developer.android.com/studio/debug/dev-options)
2. Plug the device to the computer
3. Trust the computer if it prompts. At this step, some device needs to manually enable file transfer (e.g. OPPO). You might need to google how to do that for your specific device.
4. You should now be able to see your device in “available devices”. Click “run” to deploy the app on your physical device


## Sources
* https://blog.evbox.com/far-electric-car-range
* https://www.visualcapitalist.com/visualizing-the-range-of-electric-cars-vs-gas-powered-cars/#:~:text=For%20example%2C%20in%202021%2C%20the,the%20average%20EV%20would%20cover.
* https://electrek.co/2023/01/09/heres-how-many-ev-chargers-the-us-has-and-how-many-it-needs/#:~:text=S%26P%20Global%20Mobility%20estimates%20that,20%2C431%20Level%203%20charging%20ports.
* https://www.whitehouse.gov/briefing-room/statements-releases/2023/02/15/fact-sheet-biden-harris-administration-announces-new-standards-and-major-progress-for-a-made-in-america-national-network-of-electric-vehicle-chargers/#:~:text=There%20are%20now%20more%20than,public%20chargers%20across%20the%20country.
* https://www.forbes.com/wheels/advice/ev-charging-levels/
