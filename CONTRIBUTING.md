Contributing
============

If you would like to contribute code to Metrics Integration for OkHttp you can
do so through GitHub by forking the repository and sending a pull request.

When submitting code, please make every effort to follow existing conventions
and style in order to keep the code as readable as possible.  A concrete step
you can take is to ensure you're running the following before submitting a PR:

```bash
$ cd metrics-okhttp
$ mvn clean package
```

This should run successfully.  Additionally, this runs an automated source code
formatter.  So, ensure any formatting changes that occur after running the above
commands are added to the changes in the PR.
