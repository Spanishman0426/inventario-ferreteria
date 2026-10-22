/**
 * Nombre: DigitoVerificacionNit.java
 * Descripción: Cálculo del dígito de verificación del NIT colombiano, igual a la función fn_dv_nit de la base.
 * Fecha de creación: 2026-10-22
 * Última actualización: 2026-10-22
 * Autor: Juan Pablo Artunduaga Pérez (generado con Claude Code)
 */
package co.ferreteria.inventario.tercero;

import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * Dígito de verificación del NIT (RN-26), con el mismo algoritmo de {@code fn_dv_nit} del esquema:
 * cada dígito, de derecha a izquierda, se multiplica por 3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53,
 * 59, 67 y 71; el residuo de la suma entre 11 es el dígito si vale 0 o 1, y en otro caso el dígito es
 * 11 menos el residuo. Así la aplicación rechaza un dígito incorrecto antes de que lo haga el CHECK
 * {@code ck_tercero_dv}.
 */
public final class DigitoVerificacionNit {

	/** Pesos de la DIAN, del dígito menos significativo al más significativo. */
	private static final int[] PESOS = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};

	/** Entre 1 y 15 dígitos, igual que {@code fn_dv_nit}. */
	private static final Pattern NUMERO = Pattern.compile("^[0-9]{1,15}$");

	private DigitoVerificacionNit() {
		// Clase de utilidades.
	}

	/**
	 * Calcula el dígito de verificación.
	 *
	 * @param numero NIT sin dígito de verificación ni separadores
	 * @return dígito de 0 a 9, o vacío si el número no tiene entre 1 y 15 dígitos
	 */
	public static OptionalInt calcular(String numero) {
		if (numero == null || !NUMERO.matcher(numero).matches()) {
			return OptionalInt.empty();
		}
		int suma = 0;
		int largo = numero.length();
		for (int i = 0; i < largo; i++) {
			suma += (numero.charAt(largo - 1 - i) - '0') * PESOS[i];
		}
		int residuo = suma % 11;
		return OptionalInt.of(residuo <= 1 ? residuo : 11 - residuo);
	}

	/**
	 * Indica si el dígito corresponde al número.
	 *
	 * @param numero NIT sin dígito de verificación
	 * @param digito dígito a comprobar
	 * @return {@code true} si el número es válido y el dígito coincide
	 */
	public static boolean esValido(String numero, Integer digito) {
		OptionalInt esperado = calcular(numero);
		return digito != null && esperado.isPresent() && esperado.getAsInt() == digito;
	}

}
