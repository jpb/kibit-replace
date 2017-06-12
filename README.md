:warning: This is now [available in Kibit](https://github.com/jonase/kibit#automatically-replacing-suggestions-in-source-file) 0.1.5+. Use `kibit replace` instead of this plugin. :warning: 

# kibit-replace

A Leiningen plugin to automatically apply [kibit](https://github.com/jonase/kibit) suggestions.

## Usage

Put `[kibit-replace "0.1.0"]` into the `:plugins` vector of your project.clj.

    $ lein kibit-replace # to apply all suggestions, or
    $ lein kibit-replace --interactive
      Would you like to replace
        (+ 1 a)
       with
        (inc a)
      in your_code.clj:42? [yes/no]


## License

Copyright © 2016 James Brennan

Distributed under the Eclipse Public License either version 1.0
