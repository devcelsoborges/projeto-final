#!/bin/bash

# Script de testes end-to-end para brjobs

echo "=========================================="
echo "Iniciando suite de testes E2E"
echo "=========================================="

# 1. Testes Backend (Spring Boot)
echo ""
echo "[1] Executando testes unitários do backend..."
cd brjobs-java
mvn clean test -DskipITs
if [ $? -ne 0 ]; then
    echo "❌ Testes unitários falharam!"
    exit 1
fi
echo "✅ Testes unitários passaram"

# 2. Testes de integração
echo ""
echo "[2] Executando testes de integração..."
mvn integration-test
if [ $? -ne 0 ]; then
    echo "❌ Testes de integração falharam!"
    exit 1
fi
echo "✅ Testes de integração passaram"

cd ..

# 3. Linting Frontend
echo ""
echo "[3] Executando linting Angular..."
cd brjobs-angular
npm run lint
if [ $? -ne 0 ]; then
    echo "❌ Linting Angular falhou!"
    exit 1
fi
echo "✅ ESLint passou"

# 4. Typecheck Frontend  
echo ""
echo "[4] Executando typecheck Angular..."
npm run typecheck
if [ $? -ne 0 ]; then
    echo "❌ Typecheck falhou!"
    exit 1
fi
echo "✅ Typecheck passou"

# 5. Build Frontend
echo ""
echo "[5] Compilando Angular..."
npm run build
if [ $? -ne 0 ]; then
    echo "❌ Build Angular falhou!"
    exit 1
fi
echo "✅ Build Angular sucesso"

cd ..

# 6. Relatório final
echo ""
echo "=========================================="
echo "✅ TODOS OS TESTES PASSARAM!"
echo "=========================================="
echo ""
echo "Pronto para deploy:"
echo "  - Backend JAR: brjobs-java/target/brjobs-*.jar"
echo "  - Frontend Bundle: brjobs-angular/dist/"
echo ""
