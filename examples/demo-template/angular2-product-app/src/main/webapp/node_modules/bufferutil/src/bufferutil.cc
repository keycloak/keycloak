/*!
 * bufferutil: WebSocket buffer utils
 * Copyright(c) 2015 Einar Otto Stangvik <einaros@gmail.com>
 * MIT Licensed
 */

#include <v8.h>
#include <node.h>
#include <node_version.h>
#include <node_buffer.h>
#include <node_object_wrap.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>
#include <stdio.h>
#include "nan.h"

using namespace v8;
using namespace node;

class BufferUtil : public ObjectWrap
{
public:

  static void Initialize(v8::Handle<v8::Object> target)
  {
    Nan::HandleScope scope;
    Local<FunctionTemplate> t = Nan::New<FunctionTemplate>(New);
    t->InstanceTemplate()->SetInternalFieldCount(1);
    Nan::SetMethod(t, "unmask", BufferUtil::Unmask);
    Nan::SetMethod(t, "mask", BufferUtil::Mask);
    Nan::SetMethod(t, "merge", BufferUtil::Merge);
    Nan::Set(target, Nan::New<String>("BufferUtil").ToLocalChecked(), t->GetFunction());
  }

protected:

  static NAN_METHOD(New)
  {
    Nan::HandleScope scope;
    BufferUtil* bufferUtil = new BufferUtil();
    bufferUtil->Wrap(info.This());
    info.GetReturnValue().Set(info.This());
  }

  static NAN_METHOD(Merge)
  {
    Nan::HandleScope scope;
    Local<Object> bufferObj = info[0]->ToObject();
    char* buffer = Buffer::Data(bufferObj);
    Local<Array> array = Local<Array>::Cast(info[1]);
    unsigned int arrayLength = array->Length();
    size_t offset = 0;
    unsigned int i;
    for (i = 0; i < arrayLength; ++i) {
      Local<Object> src = array->Get(i)->ToObject();
      size_t length = Buffer::Length(src);
      memcpy(buffer + offset, Buffer::Data(src), length);
      offset += length;
    }
    info.GetReturnValue().Set(Nan::True());
  }

  static NAN_METHOD(Unmask)
  {
    Nan::HandleScope scope;
    Local<Object> buffer_obj = info[0]->ToObject();
    size_t length = Buffer::Length(buffer_obj);
    Local<Object> mask_obj = info[1]->ToObject();
    unsigned int *mask = (unsigned int*)Buffer::Data(mask_obj);
    unsigned int* from = (unsigned int*)Buffer::Data(buffer_obj);
    size_t len32 = length / 4;
    unsigned int i;
    for (i = 0; i < len32; ++i) *(from + i) ^= *mask;
    from += i;
    switch (length % 4) {
      case 3: *((unsigned char*)from+2) = *((unsigned char*)from+2) ^ ((unsigned char*)mask)[2];
      case 2: *((unsigned char*)from+1) = *((unsigned char*)from+1) ^ ((unsigned char*)mask)[1];
      case 1: *((unsigned char*)from  ) = *((unsigned char*)from  ) ^ ((unsigned char*)mask)[0];
      case 0:;
    }
    info.GetReturnValue().Set(Nan::True());
  }

  static NAN_METHOD(Mask)
  {
    Nan::HandleScope scope;
    Local<Object> buffer_obj = info[0]->ToObject();
    Local<Object> mask_obj = info[1]->ToObject();
    unsigned int *mask = (unsigned int*)Buffer::Data(mask_obj);
    Local<Object> output_obj = info[2]->ToObject();
    unsigned int dataOffset = info[3]->Int32Value();
    unsigned int length = info[4]->Int32Value();
    unsigned int* to = (unsigned int*)(Buffer::Data(output_obj) + dataOffset);
    unsigned int* from = (unsigned int*)Buffer::Data(buffer_obj);
    unsigned int len32 = length / 4;
    unsigned int i;
    for (i = 0; i < len32; ++i) *(to + i) = *(from + i) ^ *mask;
    to += i;
    from += i;
    switch (length % 4) {
      case 3: *((unsigned char*)to+2) = *((unsigned char*)from+2) ^ *((unsigned char*)mask+2);
      case 2: *((unsigned char*)to+1) = *((unsigned char*)from+1) ^ *((unsigned char*)mask+1);
      case 1: *((unsigned char*)to  ) = *((unsigned char*)from  ) ^ *((unsigned char*)mask);
      case 0:;
    }
    info.GetReturnValue().Set(Nan::True());
  }
};

#if !NODE_VERSION_AT_LEAST(0,10,0)
extern "C"
#endif
void init (Handle<Object> target)
{
  Nan::HandleScope scope;
  BufferUtil::Initialize(target);
}

NODE_MODULE(bufferutil, init)
