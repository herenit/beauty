#ifndef common_smart_ptr_h
#define common_smart_ptr_h

#include <stdlib.h>
#include <memory.h>
#include <new>
#include <assert.h>

/**
 * ����ָ�룬��Ҫ�����Զ������ڴ�
 */
template <class T>
class smart_ptr {
private:
	struct Data {
		T *p;
		int refcnt;
		int sz;
		int cap;
	} *data;
	//��������
	void ensure_cap(int ncap) {
		if(data->cap >= ncap) return;
		int c = ncap & (ncap - 1);
		if(c) {
			ncap = c << 1;
		}
		T *p2 = (T*)malloc(ncap * sizeof(T));
		if(data->sz) memcpy(p2, data->p, data->sz * sizeof(T));
		free(data->p);
		data->p = p2;
		data->cap = ncap;
	}
public:
	//vector��ʽ
	smart_ptr() {
		data = new struct Data;
		data->cap = 0;
		data->p = 0;
		data->sz = 0;
		data->refcnt = 1;
	}
	//������ָ�룬ά���ڴ����������
	smart_ptr(T* ptr, int size = 0) {
		data = new struct Data;
		data->p = ptr;
		data->cap = 0;
		data->sz = size;
		data->refcnt = 1;
	}
	//����ʹ�÷�ʽ
	smart_ptr(int size) {
		data = new struct Data;
		data->p = new T[size];
        memset(data->p, 0, sizeof(T)*size);
		data->cap = 0;
		data->sz = size;
		data->refcnt = 1;
	}
	//����ʱ�ͷ��ڴ�
	~smart_ptr() {
		if(--data->refcnt > 0) return;
		if(data->cap) {
			for(int i=0; i<data->sz; i++) {
				data->p[i].~T();
			}
			free(data->p);
		} else {
			if(data->sz) {
				delete[] data->p;
			} else {
				delete data->p;
			}
		}
		delete data;
	}
	//�������죬���������ʹ�����ý��д���
	smart_ptr(const smart_ptr &rhs) {
		data = rhs.data;
		data->refcnt++;
	}
	//��ֵ
	smart_ptr & operator=(const smart_ptr &rhs) {
		if(data == rhs.data) return *this;
		this->~smart_ptr();
		data = rhs.data;
		data->refcnt++;
		return *this;
	}
	//����
	void reset(T* ptr = 0, int size = 0) {
		assert(data->cap==0);
		this->~smart_ptr();
		data = new struct Data;
		data->p = ptr;
		data->cap = 0;
		data->sz = size;
		data->refcnt = 1;
	}
	void reset(int size) {
		this->~smart_ptr();
		data = new struct Data;
		data->p = new T[size];
        memset(data->p, 0, sizeof(T)*size);
		data->cap = 0;
		data->sz = size;
		data->refcnt = 1;
	}
	//��ȡ��ָ��
	operator T*() {
		return data->p;
	}
	operator const T*() const {
		return data->p;
	}
	//������
	T& operator*() {
		return *data->p;
	}
	const T& operator*() const {
		return *data->p;
	}
	//��Ϊָ��ʹ��
	const T* operator->() const {
		return data->p;
	}
	T* operator->() {
		return data->p;
	}
	//��������飬���س��ȣ����򷵻�0
	int size() const {
		return data->sz;
	}
	//ʹ���±��ȡֵ
	const T &operator[](int idx) const {
		return data->p[idx];
	}
	T &operator[](int idx) {
		return data->p[idx];
	}
	//����
	void sort(int (*compar)(const T*,const T*), int start = 0, int end = -1) {
		if(end == -1) end = data->sz;
		if(start==end) return;
		qsort(data->p + start, end - start, sizeof(T), (int (*)(const void*, const void*))compar);
	}
	
	T* get() {
	    return data->p;
	}
};

#endif
