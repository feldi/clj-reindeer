@echo off
rmdir war-resources\VAADIN /S /Q
xcopy WebContent\VAADIN war-resources\VAADIN /I /S /Q
lein do clean, servlet run

