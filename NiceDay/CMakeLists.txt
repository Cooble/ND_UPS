#Base CMAKE
cmake_minimum_required(VERSION 3.16.1)
project("NiceDaySolution")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

set(THREADS_PREFER_PTHREAD_FLAG ON)


LIST( APPEND CMAKE_MODULE_PATH
  ${CMAKE_SOURCE_DIR}/cmake/modules
)

set_property(GLOBAL PROPERTY CTEST_TARGETS_ADDED 1) # hack to prevent CTest added targets

MARK_AS_ADVANCED(MULTITHREADED_BUILD)
set(MULTITHREADED_BUILD 8 CACHE STRING "How many threads are used to build the project")
if(${CMAKE_GENERATOR} MATCHES "Unix Makefiles")
       message(STATUS ${CMAKE_BUILD_TOOL})
       set(CMAKE_MAKE_PROGRAM "${CMAKE_MAKE_PROGRAM} -j${MULTITHREADED_BUILD}")
       message(STATUS "Added arguments to CMAKE_BUILD_TOOL: ${CMAKE_MAKE_PROGRAM}")
elseif(MSVC)
      set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /MP") #paralell 
      message(STATUS "Added parallel build arguments to CMAKE_CXX_FLAGS: ${CMAKE_CXX_FLAGS}")
endif()

# add Configurations Debug Release and Dist
if(CMAKE_CONFIGURATION_TYPES)
   list(APPEND CMAKE_CONFIGURATION_TYPES Dist)
   list(REMOVE_DUPLICATES CMAKE_CONFIGURATION_TYPES)
   list(REMOVE_ITEM CMAKE_CONFIGURATION_TYPES MinSizeRel)
   #list(REMOVE_ITEM CMAKE_CONFIGURATION_TYPES MinSizeRel RelWithDebInfo)
   message("    Available build types: ${CMAKE_CONFIGURATION_TYPES}")
   
   #add flags to config dist
   set(CMAKE_EXE_LINKER_FLAGS_DIST ${CMAKE_EXE_LINKER_FLAGS_RELEASE})
   set(CMAKE_SHARED_LINKER_FLAGS_DIST ${CMAKE_SHARED_LINKER_FLAGS_RELEASE})
  
else()
   message("    XXX custom build types are not allowed...")
endif()



#Global DEFINES based on Config
set(DEBUG_COMP_DEFINITIONS 
    ND_DEBUG
    ND_CONFIG="ND_DEBUG"
)

set(RELEASE_COMP_DEFINITIONS 
    ND_RELEASE
    ND_CONFIG="ND_RELEASE"
)

set(DIST_COMP_DEFINITIONS 
    ND_DIST
    ND_CONFIG="ND_DIST"
)

add_compile_definitions(
    "$<$<CONFIG:Debug>:${DEBUG_COMP_DEFINITIONS}>" 
    "$<$<CONFIG:Dist>:${DIST_COMP_DEFINITIONS}>" 
    "$<$<CONFIG:RelWithDebInfo>:${RELEASE_COMP_DEFINITIONS}>" 
    "$<$<CONFIG:Release>:${RELEASE_COMP_DEFINITIONS}>")

#set(CMAKE_SYSTEM_PREFIX_PATH ${CMAKE_SYSTEM_PREFIX_PATH} "./")
# Subdirs
add_subdirectory(NiceDay)
add_subdirectory(Sandbox)
