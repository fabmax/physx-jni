@echo off
cd PhysX/physx
cmake --build .\compiler\jni_vc16win64\ --config release

copy .\bin\jni.win64\release\PhysX_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXCommon_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXCooking_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXFoundation_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXJniBindings_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\

copy .\bin\jni.win64\release\PhysX_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXCommon_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXCooking_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXFoundation_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXJniBindings_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\
copy .\bin\jni.win64\release\PhysXGpu_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\
copy .\bin\win.x86_64.vc142.mt\release\PhysXDevice_64.dll ..\..\physx-jni-native-win64cuda\src\main\resources\win64\