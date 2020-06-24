package cobaia;

import cobaia.ORM.ORMAutoConvention;
import cobaia.datamapper.AreaMapper;
import cobaia.datamapper.UsuarioMapper;
import cobaia.model.Areas;
import cobaia.model.Professor;
import cobaia.model.Usuarios;
import cobaia.model.ObjectDAOTest;

public class Sparring {

	public static void main(String[] args) throws Exception {
	
		ORMAutoConvention objectorm = new ORMAutoConvention();
		UsuarioMapper usuarioMapper = new UsuarioMapper();
		AreaMapper areamapper = new AreaMapper();
		
		Usuarios usuario = new Usuarios();
		usuario.setNome("Regis Guimaraessss");
		usuario.setEmail("regisg495@gmail");
		usuario.setSenha("Aid41!845");

		Usuarios usuario2 = new Usuarios();
		usuario2.setNome("55445");
		usuario2.setEmail("regis");
		usuario2.setSenha("45454545");
		
		
		//usuarioMapper.insert(usuario2); lança exception
		
		usuarioMapper.insert(usuario);
		
		Areas area = new Areas();
		
		area.setNome("Matematica");
		
		Areas area2 = new Areas();
		area2.setNome("123456");
		//areamapper.insert(area2); lança exception
		
		areamapper.insert(area);
		
		
		ObjectDAOTest o = new ObjectDAOTest("Regis", 'M', 25, 7, 1000.0, 9.0, new Float(85.0), (float) (9.0),
		Professor.Marcio);

		objectorm.insert(o);
		
		ObjectDAOTest o2 = new ObjectDAOTest("Regis", 'M', null, 7, 1000.0, 9.0, new Float(85.0), (float) (9.0),
				Professor.Marcio);
	
	
	}
}
