node_modules:
	npm install

shadow: node_modules
	npx shadow-cljs watch app

tailwind: node_modules
	npx tailwindcss -i ./src/main.css -o ./docs/css/styles.css --watch

release: node_modules
	npx tailwindcss -i ./src/main.css -o ./docs/css/styles.css --minify
	npx shadow-cljs release app

.PHONY: shadow tailwind release
