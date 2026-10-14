# Fogón

Sistema de control de restaurantes: mesas, pedidos, cocina, menú, inventario, compras, gastos, caja, estado de resultados y reportes.

La interfaz está en español. El restaurante inicial es Fogón, en República Dominicana, con moneda DOP, zona `America/Santo_Domingo` y formato `es-DO`. Esos valores se cambian en Configuración.

## Stack

- Angular 22, TypeScript, Tailwind CSS 4 y DaisyUI 5.
- Java 21, Spring Boot 4.1, Spring Security con JWT, JPA, Bean Validation, Flyway y Springdoc.
- PostgreSQL 16.
- Maven y Docker Compose.

## Requisitos locales

- Java 21 y Maven 3.9, o el wrapper `backend/mvnw`.
- Node.js 22.22 o superior.
- PostgreSQL 16.

## Base de datos

```sql
CREATE USER restosuite WITH PASSWORD 'restosuite';
CREATE DATABASE restosuite OWNER restosuite;
```

Flyway aplica el esquema y los datos maestros al arrancar. No inserta ventas, cobros ni gastos.

## API

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/restosuite
export DB_USER=restosuite
export DB_PASSWORD=restosuite
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

La documentación interactiva queda en `http://localhost:8080/swagger-ui.html`.

## Interfaz

```bash
cd frontend
npm install
npx ng serve
```

Abra `http://localhost:4200`. El servidor de desarrollo reenvía `/api` a `http://localhost:8080`.

## Acceso de desarrollo

Estas credenciales solo sirven para el entorno local. El perfil `prod` no arranca si el secreto JWT contiene `dev-only`, tiene menos de 32 caracteres, o si la contraseña de administrador es `Admin123!` o `change-me`.

- Correo: `admin@restosuite.local`
- Contraseña: `Admin123!`

El administrador se crea en el primer arranque si ese correo no existe.

## Docker

```bash
cp .env.example .env
docker compose up --build
```

- Interfaz: `http://127.0.0.1:8088`
- API: `http://127.0.0.1:8080`
- PostgreSQL publicado solo en `127.0.0.1:5432`

## Variables

| Variable | Uso |
| --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión PostgreSQL |
| `SERVER_PORT` | Puerto de la API |
| `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` | Firma y vigencia del token |
| `CORS_ORIGINS` | Orígenes permitidos, separados por coma |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_NAME` | Administrador inicial |
| `SPRING_PROFILES_ACTIVE` | `dev` o `prod` |
| `APP_STORAGE_PATH` | Reservado para archivos locales |

## Cómo se calculan los resultados

Los ingresos se reconocen al cerrar la cuenta, no al cobrar. El ITBIS no forma parte de la venta ni del resultado.

- Ingresos netos = ventas brutas sin impuesto − descuentos − devoluciones del período en que se registran.
- Costo de productos vendidos = costo promedio de la receta al descontarla + mermas − costo reingresado por devoluciones.
- Ganancia bruta = ingresos netos − costo de productos vendidos.
- Resultado operativo = ganancia bruta − gastos operativos.
- Resultado neto = resultado operativo + otros ingresos − otros gastos.

Las compras aumentan el inventario. No se registran otra vez como gasto. Los movimientos de caja no son ingresos contables. Los precios del menú son sin impuesto, salvo que se cambie el parámetro `tax.prices_include_tax`. La receta se descuenta al enviar a cocina (`inventory.deduct_trigger=ON_SEND`) o al cerrar, según la configuración vigente en ese pedido. El redondeo monetario es half-up a 2 decimales.

## Lo que este sistema no hace

- No emite comprobantes fiscales electrónicos ni declara cumplimiento con la DGII.
- No cobra con pasarelas ni terminales bancarias. Los métodos de pago son registros internos.
- No envía comandas a impresoras ESC/POS. La cocina y los comprobantes se imprimen desde el navegador, y los comprobantes también se descargan en PDF.
- No hay carga de imágenes al servidor. El logotipo y las fotos de producto usan una URL.

## Roles

Administrador, gerente, cajero, mesero, cocinero y encargado de inventario. El backend exige el permiso en cada operación. Ocultar un botón en la interfaz no autoriza la acción.
