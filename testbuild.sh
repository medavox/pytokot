./gradlew shadowJar
cd build/libs
rm  ~/src/mine/Ysgrifen/rendering/render.py.kt
time java -jar pytokot-0.1-all.jar ~/src/mine/Ysgrifen/rendering/render.py
cd ../..
