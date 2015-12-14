Release Process
===============

 1. Update `CHANGELOG.md` with release date, new features, fixes, and next release number.
 
 2. Update `README.md` with new usage and download info.
 
 3. Commit: `git commit -am "Update project documentation for X.Y.Z."`
 
 4. Prepare: `mvn release:prepare release:perform`
