package cobaia.ORM;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import cobaia.datamapper.Conexao;

public class ORMAutoConvention extends Conexao {

	public void insert(Object object) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = object.getClass();
		List<Field> fields = new ArrayList<>();

		for (int i = 0; i < object.getClass().getDeclaredFields().length; i++) {
			fields.add(clazz.getDeclaredFields()[i]);
		}

		try (Connection connection = openConnection()) {

			String sql = "INSERT INTO " + uncapitalize(object.getClass().getSimpleName()) + " (";

			for (int i = 0; i < fields.size(); i++) {
				sql += fields.get(i).getName() + ((i == fields.size() - 1) ? ") VALUES (" : ",");
			}

			for (int i = 0; i < fields.size(); i++) {
				sql += ((i == fields.size() - 1) ? "?);" : "?,");
			}

			PreparedStatement stmt = connection.prepareStatement(sql);

			for (int i = 0; i < fields.size(); i++) {
				if (!fields.get(i).isAccessible()) {
					fields.get(i).setAccessible(true);
				}

				if (fields.get(i).get(object) == null) {
					stmt.setNull(i + 1, Types.NULL);
				} else {
					stmt.setObject(i + 1, fields.get(i).get(object));
				}
			}
			stmt.execute();
			connection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static String uncapitalize(String string) {
		if (string == null || string.length() == 0) {
			return "";
		}
		char[] chars = string.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		string = new String(chars);
		return string;
	}

}
