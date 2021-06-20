# physx-jni

[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.fabmax/physx-jni/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.fabmax/physx-jni)
![Build](https://github.com/fabmax/physx-jni/workflows/Build/badge.svg)

Java JNI bindings for Nvidia [PhysX 4.1](https://github.com/NVIDIAGameWorks/PhysX).

## How to use
The library is available on maven central, so you can easily add this to your build.gradle:
```
dependencies {
    // java bindings
    implementation("de.fabmax:physx-jni:0.4.10")
    
    // native libraries, you can add the one matching your system or both
    runtimeOnly("de.fabmax:physx-jni:0.4.10:native-win64")
    runtimeOnly("de.fabmax:physx-jni:0.4.10:native-linux64")
    
    // or with CUDA support (see notes below):
    runtimeOnly("de.fabmax:physx-jni:0.4.10:native-win64cuda")
    runtimeOnly("de.fabmax:physx-jni:0.4.10:native-linux64cuda")
}
```

## Library Coverage

The bindings include most major parts of the PhysX SDK:
- Basics
    - Static and dynamic actors
    - All geometry types (box, capsule, sphere, plane, convex mesh, triangle mesh and height field)
- All joint types (revolute, spherical, prismatic, fixed, distance and D6)
- Articulations (reduced and maximal)
- Vehicles
- Character controllers
- CUDA support

The detailed list of mapped functions is given by the interface definition file
[PhysXJs.idl](physx-jni/src/main/webidl/PhysXJs.idl). The Java classes containing the
actual bindings are generated from that file during build.

After build (or after running the corresponding gradle task `generateJniBindings`) the generated Java
classes are located under `physx-jni/src/main/generated`.

### Supported platforms:
- Windows (64-bit)
- Linux (64-bit x86)
 
Moreover, there is also a version for javascript/webassembly:
[physx-js-webidl](https://github.com/fabmax/physx-js-webidl).

### Examples
You can take a look at [HelloPhysX.java](physx-jni/src/test/java/de/fabmax/physxjni/HelloPhysX.java) for a
hello world example of how to use the library. There also are a few
[tests](https://github.com/fabmax/physx-jni/tree/main/physx-jni/src/test/java/de/fabmax/physxjni) with slightly
more advanced examples (custom simulation callbacks, triangle mesh collision, etc.).

To get a feeling of what can be done with this you can take a look at my [kool](https://github.com/fabmax/kool) demos:

> *__Note:__ These demos run directly in the browser and obviously don't use this library, but the webassembly version mentioned
> above. However, the two are functionally identical, so it shouldn't matter too much. The JNI version is much faster
> though.*

- [Ragdolls](https://fabmax.github.io/kool/kool-js/?demo=phys-ragdoll): Simple Ragdoll demo.
- [Vehicle](https://fabmax.github.io/kool/kool-js/?demo=phys-vehicle): Vehicle demo with a ray track and a few obstacles.
- [Joints](https://fabmax.github.io/kool/kool-js/?demo=phys-joints): A chain running over two gears.
- [Collision](https://fabmax.github.io/kool/kool-js/?demo=physics): Various collision shapes.

### Documentation
Unfortunately, the generated bindings currently don't include any javadoc. However, the Java API
is very close to the original PhysX C++ API, so you can simply use the official
[PhysX API documentation](https://gameworksdocs.nvidia.com/PhysX/4.1/documentation/physxapi/files/index.html) and
[PhysX User's Guide](https://gameworksdocs.nvidia.com/PhysX/4.1/documentation/physxguide/Manual/Index.html).

### Things to consider when working with native objects
Whenever you create an instance of a wrapper class within this library, this also creates an object on the native
side. Native objects are not covered by the garbage collector, so, in order to avoid a memory leak, you have to
clean up these objects yourself when you are done with them.

Here is an example:
```java
// create an object of PxVec3, this also creates a native PxVec3
// object behind the scenes.
PxVec3 vector = new PxVec3(1f, 2f, 3f);

// do something with vector...

// destroy the object once you are done with it
vector.destroy();
```

This approach has two potential problems: First, as mentioned, if you forget to call destroy(), the memory on the
native heap is not released resulting in a memory leak. Second, creating new objects on the native heap comes with
a lot of overhead and is much slower than creating a new object on the Java side.

These issues aren't a big problem for long living objects, which you create on start-up and use until you exit
the program. However, for short-lived objects like, in many cases, `PxVec3` this can have a large impact. Therefore,
there is a second method to allocate these objects: Stack allocation. To use this, you will need some sort of
memory allocator like LWJGL's MemoryStack. With that one the above example could look like this:
```java
try (MemoryStack mem = MemoryStack.stackPush()) {
    // create an object of PxVec3. The native object is allocated in memory
    // provided by MemoryStack
    PxVec3 vector = PxVec3.createAt(mem, MemoryStack::nmalloc, 1f, 2f, 3f);
    
    // do something with vector...
    // no explicit destroy needed, memory is released when we leave the scope
}
```
While the `PxVec3.createAt()` call looks a bit more complicated, this approach is much faster and comes without the
risk of leaking memory, so it should be preferred whenever possible.

### Java Callbacks

At a few places it is possible to register callbacks, e.g., `PxErrorCallback` or
`PxSimulationEventCallback`. In order to implement a callback, the corresponding Java callback class has to be
extended. The implementing class can then be passed into the corresponding PhysX API.

Here's an example how this might look:

```java
// implement callback
public class CustomErrorCallback extends JavaErrorCallback {
    @Override
    public void reportError(int code, String message, String file, int line) {
        System.out.println(code + ": " + message);
    }
}

// register / use callback
CustomErrorCallback errorCb = new CustomErrorCallback();
PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(PX_PHYSICS_VERSION, new PxDefaultAllocator(), errorCb);
```

### CUDA Support

PhysX supports accelerating physics simulation with CUDA (this, of course, requires an Nvidia GPU). Enabling
CUDA for a scene is pretty simple (but experimental!):

```java
// Setup your scene as usual
PxSceneDesc sceneDesc = new PxSceneDesc(physics.getTolerancesScale());
sceneDesc.setCpuDispatcher(PxTopLevelFunctions.DefaultCpuDispatcherCreate(8));
sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());

// Create the PxCudaContextManager
PxCudaContextManagerDesc desc = new PxCudaContextManagerDesc();
desc.setInteropMode(PxCudaInteropModeEnum.NO_INTEROP);
PxCudaContextManager cudaMgr = PxTopLevelFunctions.CreateCudaContextManager(foundation, desc);

// Check if CUDA context is valid / CUDA support is available
if (cudaMgr != null && cudaMgr.contextIsValid()) {
    // enable CUDA!
    sceneDesc.setCudaContextManager(cudaMgr);
    sceneDesc.getFlags().set(PxSceneFlagEnum.eENABLE_GPU_DYNAMICS);
    sceneDesc.setBroadPhaseType(PxBroadPhaseTypeEnum.eGPU);
    sceneDesc.setGpuMaxNumPartitions(8);
    
    // optionally fine tune amount of allocated CUDA memory
    // PxgDynamicsMemoryConfig memCfg = new PxgDynamicsMemoryConfig();
    // memCfg.setStuff...
    // sceneDesc.setGpuDynamicsConfig(memCfg);
} else {
    System.err.println("No CUDA support!");
}

// Create scene as usual
PxScene scene = physics.createScene(sceneDesc);
```

Using CUDA comes with a few implications:

The CUDA enabled native libraries are quite big (~25 MB), and I therefore decided to build a separate
set of runtime jars for them (suffixed with `cuda`, so use `de.fabmax:physx-jni:[version]:native-win64cuda` instead of
`de.fabmax:physx-jni:[version]:native-win64`).

Moreover, CUDA comes with some additional overhead (a lot of data has to be copied around between CPU and GPU). For
smaller scenes this overhead outweighs the benefits and physics computation might actually be slower than with CPU only.
I wrote a simple [CudaTest](physx-jni/src/test/java/de/fabmax/physxjni/CudaTest.java), which runs a few simulations
with an increasing number of bodies. According to this the break even point is around 5k bodies. At 20k boxes the CUDA
version runs about 3 times faster than the CPU Version (with an RTX 2080 / Ryzen 2700X). The results may be different
when using other body shapes (the test uses boxes), joints, etc.

## Building
You can build the bindings yourself:
```
# Clone this repo
git clone https://github.com/fabmax/physx-jni.git

# Enter that directory
cd physx-jni

# Generate Java/JNI code and build library
./gradlew build
```

The above code doesn't compile the native libraries but relies on the pre-built libraries located in
the platform subprojects. In order to build the native libs from source you need to clone the submodule containing
the PhysX source code, and you need python3, cmake and Visual Studio 2019 Community (on Windows) or clang (on Linux):
```
# Download submodule containing the PhysX source code
git submodule update --init

# Generate PhysX cmake project files
./gradlew generateNativeProject

# Build PhysX
./gradlew buildNativeProject
```

To make sure that the native libraries built by the instructions above are actually loaded when using the library you
should also increment the version number in the `build.gradle.kts`. By default, the library-loader code copies the
native libraries to a system temp directory and loads them from there (because, if this library is distributed as
a jar file, the native libraries inside the jar can't be directly loaded by the system). However, to avoid copying
the native libs every time, the loader checks if they already exist in the current version. Therefore, without changing
the version number, the loader might not update the native libs that are actually loaded by the system. Copying the
native libs everytime on library initialization can be forced by using a version number ending with `-SNAPSHOT`.