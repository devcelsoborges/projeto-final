import 'package:flutter_test/flutter_test.dart';

import 'package:brjobs_mobile/shared/utils/formatters.dart';
import 'package:brjobs_mobile/shared/utils/validators.dart';

void main() {
  group('Fmt.moeda', () {
    test('formata valores em BRL', () {
      expect(Fmt.moeda(1234.5), 'R\$ 1.234,50');
      expect(Fmt.moeda(0), 'R\$ 0,00');
      expect(Fmt.moeda(-12.3), '-R\$ 12,30');
    });
  });

  group('Validators', () {
    test('valida e-mail', () {
      expect(Validators.email('a@b.com'), isNull);
      expect(Validators.email('invalido'), isNotNull);
      expect(Validators.email(''), isNotNull);
    });

    test('valida nome 3-150', () {
      expect(Validators.nome('Jo'), isNotNull);
      expect(Validators.nome('João Silva'), isNull);
    });

    test('valida confirmação de senha', () {
      expect(Validators.confirmacaoSenha('abc12345', 'abc12345'), isNull);
      expect(Validators.confirmacaoSenha('abc', 'xyz'), isNotNull);
    });
  });
}
