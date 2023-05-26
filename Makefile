
all: sub-install sub-test

sub-release:
	lein sub release

sub-install:
	lein sub install

sub-test:
	lein sub test

docker-up:
	docker-compose up

docker-down:
	docker-compose down --remove-orphans

docker-rm:
	docker-compose rm --force

docker-psql:
	psql --port 35432 --host localhost -U test test

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md

pg-logs:
	tail -f '/Users/ivan/Library/Application Support/Postgres/var-14/postgresql.log'
