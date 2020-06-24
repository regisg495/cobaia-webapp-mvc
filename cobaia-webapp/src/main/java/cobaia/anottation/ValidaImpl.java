package cobaia.anottation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ValidaImpl {
	public boolean valida(Object object) throws IllegalArgumentException, IllegalAccessException {

		Class<?> clazz = object.getClass();
		List<Field> fields = new ArrayList<>();

		boolean b = false;
		for (int i = 0; i < object.getClass().getDeclaredFields().length; i++) {
			fields.add(clazz.getDeclaredFields()[i]);
		}
		for (int i = 0; i < fields.size(); i++) {
			if (!fields.get(i).isAccessible()) {
				fields.get(i).setAccessible(true);
			}
			if (fields.get(i).isAnnotationPresent(ValidaRegex.class)) {
				String regex = fields.get(i).getAnnotation(ValidaRegex.class).value();
				b = fields.get(i).get(object).toString().matches(regex);
				return b;
			}
			if (fields.get(i).isAnnotationPresent(ValidaNotNull.class) && fields.get(i).get(object) == null) {
				b = false;
				return b;
			}
			if (fields.get(i).isAnnotationPresent(ValidaLength.class)
					&& fields.get(i).get(object).toString().length() < fields.get(i).getAnnotation(ValidaLength.class)
							.min()
					&& fields.get(i).get(object).toString().length() > fields.get(i).getAnnotation(ValidaLength.class)
							.max()) {
				b = false;
				return b;
			}
		}
		return b;
	}

}