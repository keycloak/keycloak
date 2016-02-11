{
  'targets': [
    {
      'target_name': 'bufferutil',
      'include_dirs': ["<!(node -e \"require('nan')\")"],
      'cflags!': [ '-O3' ],
      'cflags': [ '-O2' ],
      'sources': [ 'src/bufferutil.cc' ]
    }
  ]
}
