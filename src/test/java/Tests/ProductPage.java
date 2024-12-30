package Tests;

import org.testng.annotations.Test;

import source.ProductSourceCode;



public class ProductPage {
	
	
	@Test
	public void searchProduct() {
		System.out.println(ProductSourceCode.search());
		

	}


}
