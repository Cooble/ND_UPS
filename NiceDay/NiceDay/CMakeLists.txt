cmake_minimum_required(VERSION 3.16.1)
project(NiceDay)

LIST( APPEND CMAKE_MODULE_PATH
  ${CMAKE_SOURCE_DIR}/cmake/modules
)


#this macro will disable package finding
    macro (nd_disable_find_package)
        set(ND_DISABLE_FIND_PACKAGE ON CACHE STRING "Disabling find_package macro" FORCE)
    endmacro()
    macro (nd_enable_find_package)
        set(ND_DISABLE_FIND_PACKAGE OFF CACHE STRING "Disabling find_package macro" FORCE)
    endmacro()

    macro(find_package)
        if(NOT ND_DISABLE_FIND_PACKAGE)
            _find_package(${ARGV})
        endif()
    endmacro()

# Source files
    file(GLOB_RECURSE SOURCES 
        "src/*.cpp" 
        "src/*.hpp" 
        "src/*.h" 
        #"vendor/glm/**.hpp"
        )
	

# Target creation
    add_library(NiceDay STATIC ${SOURCES})


# Include files
    target_include_directories(NiceDay PUBLIC 
        "src"
        "vendor/sol2/include"
        "vendor/json/include"
        "vendor/entt/include"
        "vendor/rapidxml/include"

    )

    
# Precompiled Header
   # target_precompile_headers(NiceDay PRIVATE 
    #    "src/ndpch.h"
    #    ) fuk linux gcc


# DEFINES
    target_compile_definitions(NiceDay PUBLIC 
        "_CRT_SECURE_NO_WARNINGS"
   
        "$<$<PLATFORM_ID:Windows>:ND_PLATFORM_WINDOWS>"
        "$<$<PLATFORM_ID:Linux>:ND_PLATFORM_LINUX>"
        )


# Dependencies
     # links to Niceday and sets props to be in folder called vendor
    function(link_my_lib)
        target_link_libraries(NiceDay PUBLIC ${ARGV})
        set_target_properties(${ARGV} PROPERTIES USE_FOLDERS ON FOLDER "vendor")
    endfunction()

    # adds subdir and links the lib to niceday
    function(add_my_lib folder_name) #if name of lib is same as directory
        message("Adding subdirectory: : ${folder_name}")
        add_subdirectory("vendor/${folder_name}")
        if (ARGC EQUAL 1)
            link_my_lib(${folder_name})
        else()
            link_my_lib(${ARGN})
        endif()
    endfunction()
   
    #add_custom_command(TARGET Runner POST_BUILD
    #    COMMAND ${CMAKE_COMMAND} -E copy_if_different 
    #        "${PROJECT_BINARY_DIR}/vendor/Knihovna/$<CONFIG>/Knihovna.dll"
    #        $<TARGET_FILE_DIR:Runner>)
    
    # simple libs (no config needed and directory is same as target name)

    # == glm (no need to subdir (header-only))
    #set(GLM_TEST_ENABLE OFF CACHE BOOL "" FORCE)
    #add_my_lib(glm glm_static)
    target_include_directories(NiceDay PUBLIC "vendor/glm") 

    #very important to change cxx_standard otherwise standard is 17 where certain things are deprecated and treated as error
    
    # == spdlog (no need to subdir (header-only))
    #add_subdirectory("vendor/spdlog")
    #list(APPEND EXTRA_LIBS spdlog_headers_for_ide)
    target_include_directories(NiceDay PUBLIC "vendor/spdlog/include") 


    # == nlohmann_json (no need to subdir (header-only))
    # set(JSON_BuildTests OFF CACHE BOOL "" FORCE)
    # add_subdirectory("vendor/nlohmann_json")
    target_include_directories(NiceDay PUBLIC
       "vendor/json/single_include"
    )
    #target_compile_options(NiceDay PUBLIC /bigobj)
