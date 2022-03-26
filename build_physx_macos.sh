#!/bin/sh

cd PhysX/physx
cmake --build ./compiler/jni_mac64 --config release

cp ./bin/jni.mac64/release/libPhysXJniBindings_64.dylib ../../physx-jni-natives-macos/src/main/resources/macos
