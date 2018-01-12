# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

#1.cmake verson，指定cmake版本
cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.


#2.project name，指定项目的名称，一般和项目的文件夹名称对应
PROJECT(LaneMp3)

#3.head file path，头文件目录
INCLUDE_DIRECTORIES(
src/main/cpp/include
)

#4.source directory，源文件目录
AUX_SOURCE_DIRECTORY(src/main/cpp DIR_SRCS)

#5.set environment variable，设置环境变量，编译用到的源文件全部都要放到这里，否则编译能够通过，但是执行的时候会出现各种问题，比如"symbol lookup error xxxxx , undefined symbol"
SET(TEST_MATH
${DIR_SRCS}
)
add_library( # Sets the name of the library.
             native_lame_mp3

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${DIR_SRCS} )

#6.add executable file，添加要编译的可执行文件
ADD_EXECUTABLE(${PROJECT_NAME} ${TEST_MATH})

#7.add link library，添加可执行文件所需要的库，比如我们用到了libm.so（命名规则：lib+name+.so），就添加该库的名称
target_link_libraries(${PROJECT_NAME} native_lame_mp3)