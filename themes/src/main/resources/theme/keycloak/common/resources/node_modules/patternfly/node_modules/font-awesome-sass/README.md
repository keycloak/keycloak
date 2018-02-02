# FontAwesome::Sass

[![Gem Version](https://badge.fury.io/rb/font-awesome-sass.svg)](https://badge.fury.io/rb/font-awesome-sass)

'font-awesome-sass' is a Sass-powered version of [FontAwesome](http://fortawesome.github.io/Font-Awesome/) for your Ruby projects and plays nicely with 
 Ruby on Rails, Compass, Sprockets, etc.
 
 Refactored to support more Ruby environments with code and documentation humbly used from the excellent
 [bootstrap-sass](https://github.com/twbs/bootstrap-sass) project by the Bootstrap team 

## Installation

Please see the appropriate guide for your environment of choice:

* [Ruby on Rails](#a-ruby-on-rails).
* [Compass](#b-compass-without-rails) not on Rails.

### a. Ruby on Rails

In your Gemfile include:

```ruby
gem 'font-awesome-sass', '~> 4.7.0'
```

And then execute:

```sh
bundle install
```

Import the FontAwesome styles in your `app/assets/stylesheets/application.css.scss`. The `font-awesome-sprockets` file
includes the sprockets assets helper Sass functions used for finding the proper path to the font file.

```scss
@import "font-awesome-sprockets";
@import "font-awesome";
```

#### Rails Helper usage

In your view:

```ruby
icon('flag')
# => <i class="fa fa-flag"></i>
```

```ruby
icon('flag', class: 'strong')
# => <i class="fa fa-flag strong"></i>
```

```ruby
icon('flag', 'Font Awesome', id: 'my-icon', class: 'strong')
# => <i id="my-icon" class="fa fa-flag strong"></i> Font Awesome
```

Note: the icon helper can take a hash of options that will be passed to the content_tag helper

### b. Compass without Rails

Install the gem

```sh
gem install font-awesome-sass
```

If you have an existing Compass project:

```ruby
# config.rb:
require 'font-awesome-sass'
```

Import the FontAwesome styles

```scss
@import "font-awesome-compass";
@import "font-awesome";
```

## Upgrading from FontAwesome::Sass 3.x

Prepend the `fa` class to existing icons:

3.x Syntax

```html
<i class="icon-github"></i>
```

4.x Syntax

```html
<i class="fa fa-github"></i>
```
