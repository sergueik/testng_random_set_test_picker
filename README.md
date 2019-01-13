### Info

This directory contains a custom __Random Test Picker__ class suggested in the forum [Automatically select 10% round-robin subset of the tests during run](https://automated-testing.info/t/testng-zapusk-10-testov-s-randomnoj-vyborkoj/22059/7) (in Russian).

[![BuildStatus](https://travis-ci.org/sergueik/testng_random_set_test_picker.svg?branch=master)](https://travis-ci.org/sergueik/testng_random_set_test_picker)

### Overview
![Inventory Example](https://github.com/sergueik/testng_random_set_test_picker/blob/master/screenshots/capture_test_inventory.png)

It turns out that the advice provided in the forum is incorrect at least witn __TestNg__ version __6.14.3__.

Throwing the `new` `org.testng.SkipException` from the method annotated with `@BeforeMethod` has a horrible side effect: not only one specific intended to get skipper test ends up being skipped but the exception completely aborts the flow of test execution. In other words all tests starting with the intended to skip in their normal (alphabetical, unless specified otherwise) order will be silently skipped. None of methods annotated with various `@After` scopes will be executed either.

This scenario is illustrated in this project by the class `SkipRestOfTestSetTest` in the current project. The class contains a set of dummy methods named `testTwentyOne`, `testTwentyTwo`, through `testTwentyNine`, and exercises the bad implementation of the plan to skip the "testWentyFour"
by invoking a special method `skipTestFour` in the designated __Random Test Picker__
class `TestRandomizer` that would throw the exception.

Notably the `skipTestFour` would better be named `skipAllStartingFromTestFour` since the exception side effect:
```java
public void skipTestFour(String methodName) {
  if (debug) {
    System.err.println("Examine method:   " + methodName);
  }

  if (methodName.matches(".*(?i:FOUR).*")) {
    if (debug) {
      System.err.println("Decided to skip" + methodName);
    }
    throw new SkipException("Decided to skip " + methodName);
  }
  // ... rest of the method
}
```
also affects e.g. `testTwentySix` and `testTwentyTwo` but not `testTwentyEight` or `testTwentyFive` thus confirming the default invcation order of test methods is alphabetical.

The better design allowing one e.g. skip 90% of the test set and only execute 10% randomly selected tests, is shown in example class `RandomizedSetsTest` that also utilizes `TestRandomizer`.

The tests in `RandomizedSetsTest` know their name by extending the approproate `BaseTest` (this is useful for inventory). The `TestRanomizer`'s method `decide` is now called from the every `@Test` explicitly rather then from the `@BeforeMethod`:

```java
public class RandomizedSetsTest extends BaseTest {
  @BeforeMethod
  private void setName(Method m) {
    setTestName(m.getName());
  }
  @Test(enabled = true)
  public void testOne() {
    doTest(getTestName());
  }

  public void doTest(String testName) {
    if (debug) {
      System.err.println("Called Test Ramdomizer from method.");
    }
    if (!testRandomizer.decide(testName)) {
      if (debug) {
        System.err.println(String.format("will skip %s", testName));
      }
      throw new SkipException("skipping " + testName);
    }
    assertTrue(true);
  }
```
The logic of `decide` method is currently very elementary:
```java
public boolean decide(String methodName) {
  return runAll ? true : (Math.random() > 0.01 * (float) percentage) ? false : true;
  // inventory action not shown
}
```
This strategy leads to roughly `percentage` (default is `10%`) of test methods chosen randomly to run in every run:
```sh
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running TestSuite
Configuring TestNG with: org.apache.maven.surefire.testng.conf.TestNG652Configur
ator@1175e2db
Inventory tests run:
testNine
```
subsequent run:
```sh
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running TestSuite
Configuring TestNG with: org.apache.maven.surefire.testng.conf.TestNG652Configur
ator@1175e2db
Inventory tests run:
testOne
```
### Usage

__Random Test Picker__ is a jar  artifact. In order to consume it in a Maven project, one merely needs
to add the following the dependency in the `pom` file.
```xml
<dependency>
  <groupId>com.github.sergueik.testng</groupId>
  <artifactId>testng_random_test_set</artifactId>
  <packaging>jar</packaging>
  <version>0.2-SNAPSHOT</version>
</dependency>
```
There currently is no release version, therefore one needs to also add the usual
```xml
<repositories>
  <repository>
    <id>ossrh</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </repository>
</repositories>
```
This will change after deployement of a release version.

The test methods to

### Statistics of the test method selecton

With realistic number of tests, and a stochastic `decide` method, inventory of which test methds were run, can be critical.
Finding which tests were actually run in a given run/range or runs through test log or Alure report does not scale too well.


The method `dumpInventory` is provided through which one can generate a basic YAML file to the
path specified through the property `inventoryFilePath`, overwriting the existing file, without presering historic data:
```yaml
---
testOne: false
testSeven: false
testThree: false
testSix: true
testNine: false
testFour: true
testEight: false
testFive: false
testTwo: false
testTen: true
```
### Multi-run Inventory

In addition, `TestRandomizer` hase method `updateMultiRunInventory` where it is able to create or update an Excel 2007 spreadsheet specified by caller (default is `src/test/resources/TestData.xlsx') with test names and run decision statuses (`true` means test has been run, `false` means test was skipped), optionally preserving historic data: can record the last-run or multi-run stats.

The `appendData` property, when set, makes it append a column named `Run <number>` in every new run and populate the most recent test statuses in this column, keeping the historic column intact. 

```sh
Inventory tests run: (30 %)
testThree
testNine
testSix
---
Adding extra column for run 7
0 => Test Method
1 => Run 1
2 => Run 2
3 => Run 3
4 => Run 4
5 => Run 5
6 => Run 6
7 => Run 7
---
Adding extra column for test testOne
0 => testOne
1 => false
2 => false
3 => false
4 => false
5 => false
6 => true
7 => false
...
---
Adding extra column for test testThree
0 => testThree
1 => true
2 => true
3 => true
4 => false
5 => false
6 => false
7 => true
...
```
When `appendData` is set to `false`, only the most recent run information is saved.
This feature is new, therefore by default, it is disabled.

### Work in Progress

Persistent inventory into alternative media like the database or ElasticSearch provider is a work in progress.

### See Also
 * discussion of `SkipException` in [stackoverflow](https://stackoverflow.com/questions/21591712/how-do-i-use-testng-skipexception)
 * customized TestNG [report](https://github.com/djangofan/testng-custom-report-example) illustrating skip techniques
 * [some examples copied from snakeyaml documentaion](https://www.programcreek.com/java-api-examples/?api=org.yaml.snakeyaml.DumperOptions)

### License
This project is licensed under the terms of the MIT license.

### Author
[Serguei Kouzmine](kouzmine_serguei@yahoo.com)
