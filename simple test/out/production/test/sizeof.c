#include <stddef.h>

struct MyObject {
    char str[6];
    unsigned char bool;
    int array[5];
    float fl;
};

size_t sizeof_myobject() {
    return sizeof(struct MyObject);
}
