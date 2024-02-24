package com.github.sergueik.testng.utils;
/**
 * Copyright 2019 Serguei Kouzmine
 */

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

import java.util.Optional;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.testng.SkipException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Class for execution time decision making about 
 * current testNg test methods to be run or skipped
 * implemented by throwing an exception away from the method
 * @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */

public class TestRandomizer {

	private boolean runAll;
	private boolean appendData = true;
	private boolean debug = false;
	private boolean verbose = true;
	private static DumperOptions options = new DumperOptions();
	private static Yaml yaml = null;
	private String inventoryFilePath = null;
	private static Map<String, Object> testInventoryData = new HashMap<>();
	private int percentage = 10;
	// name of the sheet - probably should infer caller class
	private static String sheetName = "Test Status";
	// format of the sheet
	private static String sheetFormat = "Excel 2007";
	// possible alternative: use RowSet like for JDBC API
	// https://docs.oracle.com/javase/7/docs/api/javax/sql/RowSet.html
	private static List<Map<Integer, String>> tableData = new ArrayList<>();
	private static Map<Integer, String> rowData = new HashMap<>();

	public static void setSheetFormat(String data) {
		TestRandomizer.sheetFormat = data;
	}

	public static void setSheetName(String data) {
		TestRandomizer.sheetName = data;
	}

	public void setAppendData(Boolean data) {
		this.appendData = data;
	}

	private String spreadsheetFilePath = null;

	private static List<String> columnHeaders = new ArrayList<>();
	private static String newColumnHeader = null;

	public void setSpreadsheetFilePath(String value) {
		this.spreadsheetFilePath = value;
	}

	public String getSpreadsheetFilePath() {
		return this.spreadsheetFilePath;
	}

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
		if (methodName.matches(".*(?i:FOUR).*")) {
			if (debug) {
				System.err.println("Decided to skip " + methodName);
			}
			throw new SkipException("Decided to skip " + methodName);
		}
		if (yaml == null) {
			loadInventory();
		}
		testInventoryData.put(methodName, true);
	}

	@SuppressWarnings("unchecked")
	public void loadInventory() {

		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		// NOTE: canonical definitely is an overkill
		// options.setCanonical(true);
		options.setExplicitStart(true);
		options.setPrettyFlow(true);
		yaml = new Yaml(options);

		try (InputStream in = Files.newInputStream(Paths.get(inventoryFilePath))) {
			// NOTE: compilation problem with snakeyaml 2.0:
			// [ERROR]     method org.yaml.snakeyaml.Yaml.<T>loadAs(java.io.InputStream,java.lang.Class<? super T>) is not applicable
			// [ERROR]       (cannot infer type-variable(s) T
			// [ERROR]         (argument mismatch; java.lang.Class<capture#1 of ? extends java.util.Map> cannot be converted to java.lang.Class<? super T>))

			// testInventoryData = yaml.loadAs(in, testInventoryData.getClass());
			testInventoryData = yaml.loadAs(in, Map.class);
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
		}
	}

	// supports last-run and keep-history invenories
	public void updateMultiRunInventory() {

		ExcelFileUtils excelFileUtils = new ExcelFileUtils();
		excelFileUtils.setSpreadsheetFilePath(spreadsheetFilePath);
		excelFileUtils.setSheetFormat(sheetFormat);
		excelFileUtils.setSheetName(sheetName);
		excelFileUtils.setDebug(true /* this.debug */ );
		excelFileUtils.setTableData(tableData);
		if (!appendData) {
			columnHeaders = excelFileUtils.readColumnHeaders();
			newColumnHeader = String.format("Run %d", columnHeaders.size());
			excelFileUtils.appendColumnHeader(newColumnHeader);

			columnHeaders = excelFileUtils.readColumnHeaders();
			if (debug) {
				System.err.println("Appended column: " + columnHeaders.toString());
			}
			rowData = new HashMap<>();
			// Note: row 0 is reserved for headers
			// for FILLO SQL query based data access
			for (int column = 0; column != columnHeaders.size(); column++) {
				rowData.put(column, columnHeaders.get(column));
			}
			tableData.add(rowData);
			// columnHeaders
			for (String testMethodName : testInventoryData.keySet()) {
				rowData = new HashMap<>();
				rowData.put(0, testMethodName);
				rowData.put(1, testInventoryData.get(testMethodName).toString());
				tableData.add(rowData);
			}
			excelFileUtils.setTableData(tableData);
		} else {
			List<Map<Integer, String>> existingData = new ArrayList<>();
			// NOTE: no need to wrap into an Optional
			excelFileUtils.readSpreadsheet(Optional.of(existingData));
			if (verbose) {
				System.err.println("Adding extra column");
			}

			tableData = new ArrayList<>(); // reset tableData
			for (Map<Integer, String> rowData : existingData) {
				String testMethodName = rowData.get(0); // "Test Method"
				Integer newColumn = rowData.size();
				if (testMethodName.matches("Test Method")) {
					// continue;
					rowData.put(rowData.size(), String.format("Run %d", newColumn));
				} else {
					if (verbose) {
						System.err
								.println("Adding extra column for test " + testMethodName);
					}
					Boolean testStatus = Boolean
							.parseBoolean(testInventoryData.get(testMethodName).toString());
					rowData.put(newColumn, testStatus.toString());
				}
				tableData.add(rowData);
				if (verbose) {
					for (Map.Entry<Integer, String> columnData : rowData.entrySet()) {
						System.err.println(columnData.getKey().toString() + " => "
								+ columnData.getValue());
					}
					System.err.println("---");
				}
			}

			excelFileUtils.setTableData(tableData);
			excelFileUtils.writeSpreadsheet();
		}
	}
}
