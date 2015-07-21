Release Process
===============

 1. Update `CHANGELOG.md` with release date, new features, fixes, and other relevant info.
 
 2. Update `README.md` with new usage and/or download info.
 
 2. Update the project version in `pom.xml`.
 
 3. Commit: `git commit -am "Prepare version X.Y.Z."`
 
 4. Tag: `git tag -a X.Y.Z -m "version X.Y.Z"`
 
 5. Update `CHANGELOG.md` with new release number to signify new feature/bug development will start.
 
 5. Update the project version in `pom.xml` back to a snapshot version.
 
 6. Commit: `git commit -am "Prepare next development version."`
 
 7. Push: `git push && git push --tags`
 
 8. :shipit:
