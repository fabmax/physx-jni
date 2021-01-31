@echo off
cd PhysX/physx
cmake --build .\compiler\jni_vc16win64\ --config release
copy .\bin\jni_windows\release\PhysX_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni_windows\release\PhysXCommon_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni_windows\release\PhysXCooking_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni_windows\release\PhysXFoundation_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
copy .\bin\jni_windows\release\PhysXJniBindings_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\
REM copy .\bin\jni_windows\release\PhysXGpu_64.dll ..\..\physx-jni-native-win64\src\main\resources\win64\