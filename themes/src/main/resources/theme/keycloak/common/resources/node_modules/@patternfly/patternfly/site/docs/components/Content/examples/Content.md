---
title: Content
section: components
cssPrefix: pf-c-content
---

## Examples
```hbs title=Basic
{{#> content}}
<h1>Hello world</h1>
<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nulla accumsan, metus ultrices eleifend gravida, nulla nunc varius
  lectus, nec rutrum justo nibh eu lectus. Ut vulputate semper dui. Fusce erat odio, sollicitudin vel erat vel, interdum
  mattis neque. Sub works as well!</p>
<h2>Second level</h2>
<p>Curabitur accumsan turpis pharetra
  <strong>augue tincidunt</strong> blandit. Quisque condimentum maximus mi, sit amet commodo arcu rutrum id. Proin pretium urna vel
  cursus venenatis. Suspendisse potenti. Etiam mattis sem rhoncus lacus dapibus facilisis. Donec at dignissim dui. Ut et
  neque nisl.</p>
<ul>
  <li>In fermentum leo eu lectus mollis, quis dictum mi aliquet.</li>
  <li>Morbi eu nulla lobortis, lobortis est in, fringilla felis.</li>
  <li>Aliquam nec felis in sapien venenatis viverra fermentum nec lectus.
    <ul>
      <li>In fermentum leo eu lectus mollis, quis dictum mi aliquet.</li>
      <li>Morbi eu nulla lobortis, lobortis est in, fringilla felis.</li>
      <li>Ut venenatis, nisl scelerisque.
        <ol>
          <li>Donec blandit a lorem id convallis.</li>
          <li>Cras gravida arcu at diam gravida gravida.</li>
          <li>Integer in volutpat libero.</li>
        </ol>
      </li>
    </ul>
  </li>
  <li>Ut non enim metus.</li>
</ul>
<h3>Third level</h3>
<p>Quisque ante lacus, malesuada ac auctor vitae, congue
  <a href="#">non ante</a>. Phasellus lacus ex, semper ac tortor nec, fringilla condimentum orci. Fusce eu rutrum tellus.</p>
<ol>
  <li>Donec blandit a lorem id convallis.</li>
  <li>Cras gravida arcu at diam gravida gravida.</li>
  <li>Integer in volutpat libero.</li>
  <li>Donec a diam tellus.</li>
  <li>Etiam auctor nisl et.
    <ul>
      <li>Donec blandit a lorem id convallis.</li>
      <li>Cras gravida arcu at diam gravida gravida.</li>
      <li>Integer in volutpat libero.
        <ol>
          <li>Donec blandit a lorem id convallis.</li>
          <li>Cras gravida arcu at diam gravida gravida.</li>
        </ol>
      </li>
    </ul>
  </li>
  <li>Aenean nec tortor orci.</li>
  <li>Quisque aliquam cursus urna, non bibendum massa viverra eget.</li>
  <li>Vivamus maximus ultricies pulvinar.</li>
</ol>
<blockquote>Ut venenatis, nisl scelerisque sollicitudin fermentum, quam libero hendrerit ipsum, ut blandit est tellus sit amet turpis.</blockquote>
<p>Quisque at semper enim, eu hendrerit odio. Etiam auctor nisl et
  <em>justo sodales</em> elementum. Maecenas ultrices lacus quis neque consectetur, et lobortis nisi molestie.</p>
<hr>
<p>Sed sagittis enim ac tortor maximus rutrum. Nulla facilisi. Donec mattis vulputate risus in luctus. Maecenas vestibulum interdum
  commodo.
</p>
<dl>
  <dt>Web</dt>
  <dd>The part of the internet that contains websites and web pages</dd>
  <dt>HTML</dt>
  <dd>A markup language for creating web pages</dd>
  <dt>CSS</dt>
  <dd>A technology to make HTML look better</dd>
</dl>
<p>Suspendisse egestas sapien non felis placerat elementum. Morbi tortor nisl, suscipit sed mi sit amet, mollis malesuada nulla.
  Nulla facilisi. Nullam ac erat ante.</p>
<h4>Fourth level</h4>
<p>Nulla efficitur eleifend nisi, sit amet bibendum sapien fringilla ac. Mauris euismod metus a tellus laoreet, at elementum
  ex efficitur.</p>
<p>Maecenas eleifend sollicitudin dui, faucibus sollicitudin augue cursus non. Ut finibus eleifend arcu ut vehicula. Mauris
  eu est maximus est porta condimentum in eu justo. Nulla id iaculis sapien.</p>
<small>Sometimes you need small text to display things like date created</small>
<p>Phasellus porttitor enim id metus volutpat ultricies. Ut nisi nunc, blandit sed dapibus at, vestibulum in felis. Etiam iaculis
  lorem ac nibh bibendum rhoncus. Nam interdum efficitur ligula sit amet ullamcorper. Etiam tristique, leo vitae porta faucibus,
  mi lacus laoreet metus, at cursus leo est vel tellus. Sed ac posuere est. Nunc ultricies nunc neque, vitae ultricies ex
  sodales quis. Aliquam eu nibh in libero accumsan pulvinar. Nullam nec nisl placerat, pretium metus vel, euismod ipsum.
  Proin tempor cursus nisl vel condimentum. Nam pharetra varius metus non pellentesque.</p>
<h5>Fifth level</h5>
<p>Aliquam sagittis rhoncus vulputate. Cras non luctus sem, sed tincidunt ligula. Vestibulum at nunc elit. Praesent aliquet
  ligula mi, in luctus elit volutpat porta. Phasellus molestie diam vel nisi sodales, a eleifend augue laoreet. Sed nec eleifend
  justo. Nam et sollicitudin odio.</p>
<h6>Sixth level</h6>
<p>Cras in nibh lacinia, venenatis nisi et, auctor urna. Donec pulvinar lacus sed diam dignissim, ut eleifend eros accumsan.
  Phasellus non tortor eros. Ut sed rutrum lacus. Etiam purus nunc, scelerisque quis enim vitae, malesuada ultrices turpis.
  Nunc vitae maximus purus, nec consectetur dui. Suspendisse euismod, elit vel rutrum commodo, ipsum tortor maximus dui,
  sed varius sapien odio vitae est. Etiam at cursus metus.</p>
{{/content}}
```

## Documentation
### Overview
When you can't use the CSS classes you want, or when you just want to directly use HTML tags, use `pf-c-content` as container. It can handle almost any HTML tag:

- `<p>` paragraphs
- `<ul>` `<ol>` `<dl>` lists
- `<h1>` to `<h6>` headings
- `<blockquote>` quotes
- `<em>` and `<strong>`

This `pf-c-content` class can be used in any context where you just want to (or can only) write some text.

This component is an exception to the variable system since we style type selectors.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-content` | `<div>`, `<section>`, or `<article>` | Generates vertical rythm and typographic treatment to html elements |
