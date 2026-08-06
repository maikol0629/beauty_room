# Backup y restauración de MariaDB para Beauty Room

## Backup básico

```bash
mkdir -p backups
docker exec db-1 mysqldump -u dev -pdev123 beauty_room > backups/beauty_room_$(date +%F_%H%M%S).sql
```

## Restauración básica

```bash
docker exec -i db-1 mysql -u dev -pdev123 beauty_room < backups/beauty_room.sql
```

## Recomendaciones

- Mantener copias diarias en un sistema externo.
- Probar restauraciones periódicamente.
- No depender solo del contenedor local.
