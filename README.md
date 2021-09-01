# ijuncrustify/ijuncrustify

## Getting Started

Download links:

SSH clone URL: ssh://git@git.jetbrains.team/ijuncrustify/ijuncrustify.git

HTTPS clone URL: https://git.jetbrains.team/ijuncrustify/ijuncrustify.git

## Usage

To use Uncrustify to format files written in languages it supports, you must first enable it in Settings | Editor | Code Style and set path to an Uncrustify executable in Settings | Tools | Uncrustify. The plugin checks for file extensions, to decide whether files can be formatted using Uncrustify. Uncrustify is only able to reformat whole files (i.e. it will not be invoked when formatting selections). 

## Configuration Files

There are three possibilities when the plugin selects an Uncrustify configuration file to be used for formatting:
1. File named `uncrustify.cfg` in project folder.
2. If `uncrustify.cfg` cannot be found in the project folder, custom file specified in `Settings | Tools | Uncrustify` is used.
3. If neither `uncrustify.cfg` nor custom file are specified, a temporary configuration file is generated from most suitable IntelliJ code style settings. 

Please note that generated Uncrustify options are not perfect and never will be. Generated Uncrustify configuration files can however serve as a starting point for fine-tuning. To generate a config file and write it to a custom location, click the gear icon next to the scheme field in `Settings | Editor | Code Style` and select `Export > Uncrustify config file`.

## Tests

To run tests, you need to first pass a path to an Uncrustify executable to the tested Java process. 
You can specify it in build.gradle by modifying the `test` task:
```
test {
    systemProperty('uncrustify.executablePath', '<your path to Uncrustify executable>')
    useJUnitPlatform()
}
```

## Development Tools

There is a tool, that visualises differences between IntelliJ and Uncrustify formatting using a diff window. 

To enable it, set uncrustify.useDevTools system property to true. (Either by modifying build.gradle, or by passing it to gradle task using `-D=...` in run configuration settings, it will get propagated by build.gradle to the IDE process).

Then, you should see `Uncrustify Config Format Diff` item in `Tools` menu. The tool formats currently selected file and uses the same rules for which configuration file to use.

## Resources

Project page: https://jetbrains.team/p/ijuncrustify

Uncrustify github: https://github.com/uncrustify/uncrustify


