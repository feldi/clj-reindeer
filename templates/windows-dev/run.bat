@echo off
rem rmdir war-resources\VAADIN /S /Q
rem xcopy WebContent\VAADIN war-resources\VAADIN /I /S /Q
lein servlet run

