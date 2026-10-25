package com.jasonwjones.hyperion.thriller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class MdxResultTest {

	@Test
	public void splitsHeaderRowsFromDataRows() throws ThrillerException {
		MdxResult result = Fixtures.result("measures-by-month");
		assertEquals(Arrays.asList("Measures"), result.getRowDimensions());
		assertEquals(Arrays.asList("Year", "Product"), result.getColumnDimensions());
		assertEquals(2, result.getHeaderRows().size());
		assertEquals(Arrays.asList("", "Jan", "Feb", "Mar"), result.getHeaderRows().get(0));
		assertEquals(Arrays.asList("Sales", "4860.0", "4821.0", "4904.0"), result.getDataRows().get(0));
		assertEquals(4, result.getWidth());
	}

	@Test
	public void readsRowsWithTwoRowDimensions() throws ThrillerException {
		MdxResult result = Fixtures.result("missing-values");
		assertEquals(Arrays.asList("Market", "Product"), result.getRowDimensions());
		assertEquals(9, result.getDataRows().size());
		List<String> nevadaCaffeineFree = result.getDataRows().get(5);
		assertEquals(Arrays.asList("Nevada", "100-30", "#Missing"), nevadaCaffeineFree);
	}

	@Test
	public void treatsEveryRowAsDataWhenNothingIsOnTheColumns() throws ThrillerException {
		MdxResult result = Fixtures.result("dimension-properties");
		assertEquals(0, result.getHeaderRows().size());
		assertEquals(Arrays.asList("100-10", "Cola", "0"), result.getDataRows().get(2));
	}

	@Test
	public void readsEssbase21sShapeOfTheSameResult() throws ThrillerException {
		// 21 sends the page dimensions as plain names and 26.1 as objects; both are ignored
		MdxResult result = Fixtures.result("essbase-21-two-row-dimensions");
		assertEquals(Arrays.asList("Market", "Product"), result.getRowDimensions());
		assertEquals(Arrays.asList("East", "100-10", "1812.0", "1754.0"), result.getDataRows().get(0));
	}

	@Test
	public void rejectsSomethingThatIsNotJson() {
		assertThrows(ThrillerException.class, () -> MdxResult.fromJson("<html>Not Found</html>"));
	}

}
