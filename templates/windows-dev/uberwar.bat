rmdir war-resources\VAADIN /S /Q
xcopy WebContent\VAADIN war-resources\VAADIN /I /S /Q
lein2 do clean, deps, ring uberwar

