cmake_minimum_required(VERSION 3.22.1)

project("smartbasketball")

add_library(smartbasketballgpio SHARED
    native-lib.cpp)

target_link_libraries(smartbasketballgpio
    android
    log)
