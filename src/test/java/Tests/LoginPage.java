package Tests;



import org.testng.annotations.Test;

import source.LoginSourceCode;




public class LoginPage {

	@Test
	public void Login() {
		System.out.println(LoginSourceCode.Login());
		//Assert.assertEquals(sourceCode1.start(), "start");

	}

	@Test
	public void Signup() {
		//Assert.assertEquals(sourceCode1.stop(), "stop");
		System.out.println(LoginSourceCode.SignUp());
	}
	@Test
	public void Home() {
		System.out.println(LoginSourceCode.Home());
	}
	@Test
	public void welcome() {
		System.out.println("Hello everyone");
	}

}