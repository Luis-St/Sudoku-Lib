package net.luis.sudoku.version;

import org.junit.jupiter.api.Test;

import java.lang.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link GenVersion}.
 */
class GenVersionTest {
	
	@Test
	void current_theVersionStamp_isAtLeastOne() {
		assertTrue(GenVersion.CURRENT >= 1, "CURRENT was " + GenVersion.CURRENT);
	}
	
	@Test
	void current_theField_isPublicStaticFinalInt() throws Exception {
		Field field = GenVersion.class.getDeclaredField("CURRENT");
		assertAll(
			() -> assertEquals(int.class, field.getType()),
			() -> assertTrue(Modifier.isPublic(field.getModifiers())),
			() -> assertTrue(Modifier.isStatic(field.getModifiers())),
			() -> assertTrue(Modifier.isFinal(field.getModifiers()))
		);
	}
	
	@Test
	void class_theUtilityType_isFinal() {
		assertTrue(Modifier.isFinal(GenVersion.class.getModifiers()));
	}
	
	@Test
	void constructor_theOnlyConstructor_isPrivate() {
		Constructor<?>[] constructors = GenVersion.class.getDeclaredConstructors();
		assertAll(
			() -> assertEquals(1, constructors.length),
			() -> assertEquals(0, constructors[0].getParameterCount()),
			() -> assertTrue(Modifier.isPrivate(constructors[0].getModifiers()))
		);
	}
	
	@Test
	void constructor_newInstanceWithoutAccess_throws() throws Exception {
		Constructor<GenVersion> constructor = GenVersion.class.getDeclaredConstructor();
		assertThrows(IllegalAccessException.class, constructor::newInstance);
	}
}
