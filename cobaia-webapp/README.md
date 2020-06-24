# Avaliações

As avaliações serão listadas aqui. Implemente-as na sua cópia do app.

## ava-00-model-cadastrar-area

Com base no registro de usuário (ver `Spark.get("/registro",` e `Spark.post("/registro",`) criar o registro de área.

Deve apresentar um formulário (ver `resources/templates/registro.pebble`) e permitir a submissão segundo um número mínimo de caracteres (validar no front-end (ver propriedade HTML `minlength`) e no back-end (ver registro de usuário)).

Deve existir uma classe `Area`, embora a validação possa ficar no `main`, por enquanto.


## ava-01-transaction-script-and-datamapper

Criar um _Transaction Script_ e um _Data Mapper_ para `Area`.


## ava-02-orm-auto-convention-over-configuration

Criar uma classe que permita persistir qualquer objeto desde que haja uma tabela e colunas homônimas às classes e atributos. Sugestão de API:

```java
Usuario u = new Usuario();
u.setNome("John Doe");
u.setEmail("john@doe.me");
u.setSenha("1234");

Database db = new Database();
db.insert(u);
```

Desafio: usar _annotations_ para configuração quando não seguem mesmos nomes (opcional);


## ava-03-validacao-por-metadados

Usar _annotations_ para validar os _models_, por exemplo:

```java
public class Professor {
  @ValidateLength(min=5, max=50)
  private String nome;
  @ValidateEmail
  private String email;
  @ValidateCPF
  private String CPF;
}
```

Os nomes anteriores são apenas uma demonstração, não é obrigado segui-los. A solução pode ser inspirada na biblioteca [_Bean Validation_](http://lmgtfy.com/?q=bean+validation).

# ava-04-testagem

Criar ou reusar do semestre passado uma classe `Time` e escrever os Testes Unitários para ela, dividindo os grupos de operações em Casos de Teste. Ver exemplo em _aula-05-testagem_.

No projeto aula-06, substitua os Fake Objects por Mock Objects. Fica a sua escolha a biblioteca de Mockery (_Mockagem_).
