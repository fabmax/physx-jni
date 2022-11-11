@echo off
cd PhysX/physx
cmake --build .\compiler\jni-vc16win64\ --config release

if not exist "..\..\physx-jni-natives-windows\src\main\resources\windows\" mkdir ..\..\physx-jni-natives-windows\src\main\resources\windows\
if not exist "..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\" mkdir ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\

copy .\bin\UNKNOWN\release\PhysX_64.dll ..\..\physx-jni-natives-windows\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXCommon_64.dll ..\..\physx-jni-natives-windows\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXCooking_64.dll ..\..\physx-jni-natives-windows\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXFoundation_64.dll ..\..\physx-jni-natives-windows\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXJniBindings_64.dll ..\..\physx-jni-natives-windows\src\main\resources\windows\

copy .\bin\UNKNOWN\release\PhysX_64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXCommon_64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXCooking_64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXFoundation_64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXJniBindings_64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\
copy .\bin\UNKNOWN\release\PhysXGpu_64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\
copy .\bin\win.x86_64.vc142.mt\release\PhysXDevice64.dll ..\..\physx-jni-natives-windows-cuda\src\main\resources\windows\