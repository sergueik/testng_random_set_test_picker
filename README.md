### Info

This directory contains a custom Randomize Test class suggested in the forum [Automatically select 10% round-robin subset of the tests during run](https://automated-testing.info/t/testng-zapusk-10-testov-s-randomnoj-vyborkoj/22059/7) (in Russian).

[![BuildStatus](https://travis-ci.org/sergueik/testng_random_set_test_picker.svg?branch=master)](https://travis-ci.org/sergueik/testng_random_set_test_picker)

### Overview

It turns out that the advice provided in the forum is incorrect at least witn __TestNg__ version __6.14.3__.

Throwing the `new` `org.testng.SkipException` from the method annotated with `@BeforeMethod` has a horrible side effect: not only one specific intended to get skipper test ends up being skipped but the exception completely aborts the flow of test execution. In other words all tests starting with the intended to skip in their normal (alphabetical, unless specified otherwise) order will be silently skipped. None of methods annotated with various `@After` scopes will be executed either.

This scenario is illustrated in this project by the class `SkipRestOfTestSetTest` in the current project. The class contains a set of dummy methods named `testTwentyOne`, `testTwentyTwo`, through `testTwentyNine`, and exercises the bad implementation of the plan to skip the "testWentyFour" by invoking a special method `skipTestFour` in the designated class `TestRandomizer` class that would throw the exception.

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
### Work in Progress

With the realistic number of tests, and a random `decide` method, inventory  is critical: scanning test log or Alure report does not scale too well.
With the method `dumpInventory` one can currently write the YAML file to the
path specified through the property `inventoryFilePath`, overwriting that file, without presering the historic data:
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
`TestRandomizer` also creates an Excel 2007 spreadsheet `src/test/resources/TestData..xlsx' with test names and statuses in a new column (every run adds a column). This is wotk in progress.

Persisten: inventory into a csv/spreadsheet/ELK is a work in progress.

### See Also
 * discussion of `SkipException` in [stackoverflow](https://stackoverflow.com/questions/21591712/how-do-i-use-testng-skipexception)
 * customized TestNG [report](https://github.com/djangofan/testng-custom-report-example) illustrating skip techniques
 * [some examples copied from snakeyaml documentaion](https://www.programcreek.com/java-api-examples/?api=org.yaml.snakeyaml.DumperOptions)

### License
This project is licensed under the terms of the MIT license.

### Author
[Serguei Kouzmine](kouzmine_serguei@yahoo.com)
