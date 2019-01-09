package com.github.sergueik.testng.utils;

/**
 * Copyright 2019 Serguei Kouzmine
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.SkipException;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.codoid.products.fillo.Connection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
	private static String excelFileName = null; // name of excel file
	private static String sheetName = "Sheet1"; // name of the sheet
	private static String sheetFormat = "Excel 2007"; // format of the sheet
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

	public static void setExcelFileName(String data) {
		TestRandomizer.excelFileName = data;
	}

	private String spreadsheetFilePath = System.getProperty("user.dir")
			+ "/src/test/resources/TestData.xlsx";

	private final String saveFilePath = System.getProperty("user.dir")
			+ "/src/test/resources/TestData.BACKUP.xlsx";
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
		}
		columnHeaders = readColumnHeaders();
		newColumnHeader = String.format("Column%d", columnHeaders.size());
		appendColumnHeader(newColumnHeader);
		columnHeaders = readColumnHeaders();
		if (debug) {
			System.err.println("Appended new column: " + columnHeaders.toString());
		}
		setExcelFileName(spreadsheetFilePath);
		setSheetName("Test Status");
		setSheetFormat("Excel 2007");
		rowData = new HashMap<>();
		// TODO: reserve row 0 for headers so it can be queried via FILLO SQL
		for (int column = 0; column != columnHeaders.size(); column++) {
			rowData.put(column, columnHeaders.get(column));
		}
		tableData.add(rowData);
		// columnHeaders
		for (String testMethodName : testInventoryData.keySet()) {
			rowData = new HashMap<>();
			// TODO: take into account header offset
			rowData.put(0, testMethodName);
			rowData.put(1, testInventoryData.get(testMethodName).toString());
			tableData.add(rowData);
		}
		setTableData(tableData);
		try {
			readSpreadsheet();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			writeSpreadsheet();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> readColumnHeaders() {

		List<String> result = new ArrayList<>();
		Map<String, String> columns = new HashMap<>();
		XSSFWorkbook workBook = null;
		try {
			workBook = new XSSFWorkbook(spreadsheetFilePath);
		} catch (IOException e) {
		}
		if (workBook == null) {
			return new ArrayList<>();
		}
		XSSFSheet sheet = workBook.getSheetAt(0);
		Iterator<Row> rows = sheet.rowIterator();
		while (rows.hasNext()) {
			XSSFRow row = (XSSFRow) rows.next();
			if (row.getRowNum() == 0) {
				Iterator<org.apache.poi.ss.usermodel.Cell> cells = row.cellIterator();
				while (cells.hasNext()) {

					XSSFCell cell = (XSSFCell) cells.next();
					int columnIndex = cell.getColumnIndex();
					String columnHeader = cell.getStringCellValue();
					String columnName = CellReference
							.convertNumToColString(cell.getColumnIndex());
					columns.put(columnName, columnHeader);
					if (debug) {
						System.err
								.println(columnIndex + "[" + columnName + "]: " + columnHeader);
					}
				}
			}
		}
		try {
			workBook.close();
			result = new ArrayList<String>(columns.values());
		} catch (IOException e) {
			System.err.println("Exception (ignored): " + e.toString());
		}
		if (debug) {
			System.err.println("Return: " + result.toString());
		}
		return result;
	}

	private void appendColumnHeader(String columnHeader) {
		XSSFWorkbook workBook = null;
		try {
			workBook = new XSSFWorkbook(spreadsheetFilePath);
		} catch (IOException e) {
		}
		if (workBook == null) {
			return;
		}
		XSSFSheet sheet = workBook.getSheetAt(0);
		Iterator<Row> rows = sheet.rowIterator();
		Iterator<org.apache.poi.ss.usermodel.Cell> cells;
		while (rows.hasNext()) {
			XSSFRow row = (XSSFRow) rows.next();
			if (row.getRowNum() == 0) {
				XSSFCell cell = row.createCell(columnHeaders.size());
				cell.setCellValue(columnHeader);
				if (debug) {
					System.err.println("Adding column # " + (columnHeaders.size())
							+ " with the name: " + columnHeader);
				}
				cells = row.cellIterator();
			}
		}
		try {
			// TODO: saving "it" properly, without dummy temporary file
			FileOutputStream fileOut = new FileOutputStream(saveFilePath);
			workBook.write(fileOut);
			fileOut.flush();
			fileOut.close();

			workBook.close();
		} catch (IOException e) {
		}
		File file = new File(saveFilePath);
		if (debug) {
			if (file.delete()) {
				System.out.println("File " + saveFilePath + " deleted successfully");
			} else {
				System.out.println("Failed to delete the file " + saveFilePath);
			}
		}
	}

	public static void setTableData(List<Map<Integer, String>> data) {
		tableData = data;
	}

	// TODO: Optional argument to store the data read
	private void readSpreadsheet() throws IOException {
		if (sheetFormat.matches("(?i:Excel 2007)")) {
			if (debug) {
				System.err.println("Reading Excel 2007 data sheet.");
			}
			readXLSXFile();
		} else if (sheetFormat.matches("(?i:Excel 2003)")) {
			if (debug) {
				System.err.println("Reading Excel 2003 data sheet.");
			}
			readXLSFile();
		} else {
			if (debug) {
				System.err.println("Unrecognized data sheet format: " + sheetFormat);
			}
		}
	}

	private void readXLSFile() throws IOException {

		InputStream ExcelFileToRead = new FileInputStream(excelFileName);
		HSSFWorkbook wb = new HSSFWorkbook(ExcelFileToRead);
		HSSFSheet sheet = wb.getSheetAt(0);
		HSSFRow row;
		HSSFCell cell;

		Iterator<Row> rows = sheet.rowIterator();

		while (rows.hasNext()) {

			row = (HSSFRow) rows.next();
			Iterator<Cell> cells = row.cellIterator();

			while (cells.hasNext()) {

				cell = (HSSFCell) cells.next();
				CellType type = cell.getCellTypeEnum();

				if (type == org.apache.poi.ss.usermodel.CellType.STRING) {
					System.err.println(cell.getStringCellValue() + " ");
				} else if (type == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
					System.err.println(cell.getNumericCellValue() + " ");
				} else {
					System.err.println("? ");
					// TODO: Boolean, Formula, Errors
				}
			}
		}
	}

	private void readXLSXFile() throws IOException {

		InputStream ExcelFileToRead = new FileInputStream(excelFileName);
		XSSFWorkbook wb = new XSSFWorkbook(ExcelFileToRead);
		XSSFWorkbook test = new XSSFWorkbook();
		XSSFSheet sheet = wb.getSheetAt(0);
		XSSFRow row;
		XSSFCell cell;
		Iterator<Row> rows = sheet.rowIterator();
		while (rows.hasNext()) {
			row = (XSSFRow) rows.next();
			Iterator<Cell> cells = row.cellIterator();
			while (cells.hasNext()) {
				cell = (XSSFCell) cells.next();
				CellType type = cell.getCellTypeEnum();
				if (type == org.apache.poi.ss.usermodel.CellType.STRING) {
					System.err.println(cell.getStringCellValue() + " ");
				} else if (type == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
					System.err.println(cell.getNumericCellValue() + " ");
				} else {
					// TODO: Boolean, Formula, Errors
					System.err.println("? ");
				}
			}
			System.err.println("");
		}
	}

	private void writeSpreadsheet() throws IOException {
		if (sheetFormat.matches("(?i:Excel 2007)")) {
			if (debug) {
				System.err.println("Reading Excel 2007 data sheet.");
			}
			writeXLSXFile();
		} else if (sheetFormat.matches("(?i:Excel 2003)")) {
			if (debug) {
				System.err.println("Reading Excel 2003 data sheet.");
			}
			writeXLSFile();
		} else {
			if (debug) {
				System.err.println("Unrecognized data sheet format: " + sheetFormat);
			}
		}

	}
	
	private void writeXLSFile() throws IOException {

		HSSFWorkbook wbObj = new HSSFWorkbook();
		HSSFSheet sheet = wbObj.createSheet(sheetName);

		for (int row = 0; row < tableData.size(); row++) {
			HSSFRow rowObj = sheet.createRow(row);
			rowData = tableData.get(row);
			for (int col = 0; col < rowData.size(); col++) {
				HSSFCell cellObj = rowObj.createCell(col);
				cellObj.setCellValue(rowData.get(col));
			}
		}

		FileOutputStream fileOut = new FileOutputStream(excelFileName);
		wbObj.write(fileOut);
		wbObj.close();
		fileOut.flush();
		fileOut.close();
	}

	private void writeXLSXFile() throws IOException {

		// @SuppressWarnings("resource")
		XSSFWorkbook wbObj = new XSSFWorkbook();
		XSSFSheet sheet = wbObj.createSheet(sheetName);
		for (int row = 0; row < tableData.size(); row++) {
			XSSFRow rowObj = sheet.createRow(row);
			rowData = tableData.get(row);
			for (int col = 0; col < rowData.size(); col++) {
				XSSFCell cell = rowObj.createCell(col);
				cell.setCellValue(rowData.get(col));
				System.err
						.println("Writing " + row + " " + col + "  " + rowData.get(col));
			}
		}
		FileOutputStream fileOut = new FileOutputStream(excelFileName);
		wbObj.write(fileOut);
		wbObj.close();
		fileOut.flush();
		fileOut.close();
	}

}
