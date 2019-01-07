package com.github.sergueik.testng.utils;

import java.io.FileWriter;
/**
 * Copyright 2019 Serguei Kouzmine
 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.testng.SkipException;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Class for skipping a specific testNg test execution by throwing an exception  
 * @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */

public class TestRandomizer {

	private boolean runAll;
	private boolean debug = false;
	private boolean verbose = false;
	private static DumperOptions options = new DumperOptions();
	private static Yaml yaml = null;
	private String inventoryFilePath = null;
	private static Map<String, Object> testInventoryData = new HashMap<>();
	private int percentage = 10;

	public void setInventoryFilePath(String value) {
		this.inventoryFilePath = value;
	}

	public void setDebug(boolean value) {
		this.debug = value;
	}

	public void setVerbose(boolean value) {
		this.verbose = value;
	}

	public void setRunAll(boolean value) {
		this.runAll = value;
	}

	// TestRandomizer does not have to be a singleton
	private static TestRandomizer instance = new TestRandomizer();

	private TestRandomizer() {
	}

	public static TestRandomizer getInstance() {
		return instance;
	}

	// https://stackoverflow.com/questions/21591712/how-do-i-use-testng-skipexception
	public void decide(String methodName) {
		boolean status = runAll ? true
				: (Math.random() > 0.01 * (float) percentage) ? false : true;
		if (yaml == null) {
			loadInventory();
		}
		testInventoryData.put(methodName, status);
		if (debug) {
			System.err.println(
					"Decided to " + (status ? "run" : "skip") + " " + methodName);
		}
		if (!status) {
			throw new SkipException("Skipping " + methodName);
		}
	}

	public int getPercentage() {
		return percentage;
	}

	public void setPercentage(int percentage) {
		this.percentage = percentage;
	}

	// Example to illustrate that throw SkipException from @Before stops the
	// specific test and also quits to none of
	// subsequent tests would be run
	public void skipTestFour(String methodName) {

		if (debug) {
			System.err.println("Examine method: 	" + methodName);
		}
		if (methodName.matches("(?i).*four.*")) {
			if (debug) {
				System.err.println("Decided to skip" + methodName);
			}
			throw new SkipException("Decided to skip " + methodName);
		}
		if (yaml == null) {
			loadInventory();
		}
		testInventoryData.put(methodName, true);
	}

	public void loadInventory() {

		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		// NOTE: canonical definitely is an overkill
		// options.setCanonical(true);
		options.setExplicitStart(true);
		options.setPrettyFlow(true);
		yaml = new Yaml(options);

		try (InputStream in = Files.newInputStream(Paths.get(inventoryFilePath))) {
			testInventoryData = yaml.loadAs(in, testInventoryData.getClass());
		} catch (IOException e) {
			e.printStackTrace();
		}
		testInventoryData.keySet().forEach(t -> testInventoryData.put(t, false));
	}

	// origin:
	// https://stackoverflow.com/questions/33459961/how-to-filter-a-map-by-its-values-in-java-8
	static <K, V> Map<K, V> filterByValue(Map<K, V> map, Predicate<V> predicate) {
		return map.entrySet().stream()
				.filter(entry -> predicate.test(entry.getValue()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	public List<String> listExecutedTests() {

		Map<String, Object> filteredExecutedTests = filterByValue(testInventoryData,
				value -> value
						.equals(true) /*	Boolean.parseBoolean(value.toString()) */ );
		return new ArrayList<String>(filteredExecutedTests.keySet());
	}

	public void printInventory() {
		System.err
				// literal percent sign escaped with percent sign
				.println(String.format("Inventory tests run: (%d %%)", percentage));
		listExecutedTests().stream().forEach(System.err::println);
	}

	// NOTE: loses the comments in the checked-in inventory YAML example path
	public void dumpInventory() {
		FileWriter writer;
		try {
			writer = new FileWriter(inventoryFilePath);
			yaml.dump(testInventoryData, writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
			// e.printStackTrace();
		}
	}
}