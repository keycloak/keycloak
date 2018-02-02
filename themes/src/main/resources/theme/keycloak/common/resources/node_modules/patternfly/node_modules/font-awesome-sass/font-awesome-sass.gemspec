# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

require 'font_awesome/sass/version'

Gem::Specification.new do |spec|
  spec.name          = 'font-awesome-sass'
  spec.version       = FontAwesome::Sass::VERSION
  spec.authors       = ['Travis Chase']
  spec.email         = ['travis@travischase.me']
  spec.description   = 'Font-Awesome SASS gem for use in Ruby projects'
  spec.summary       = 'Font-Awesome SASS'
  spec.homepage      = 'https://github.com/FortAwesome/font-awesome-sass'
  spec.license       = 'MIT'
  spec.files         = `git ls-files`.split($/)
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ['lib']

  spec.add_runtime_dependency 'sass', '>= 3.2'

  spec.add_development_dependency 'bundler', '>= 1.3'
  spec.add_development_dependency 'rake'
  spec.add_development_dependency 'sass-rails'
  spec.add_development_dependency 'compass'
end
