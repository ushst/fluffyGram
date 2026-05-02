/*-
 * Copyright 2003-2005 Colin Percival
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted providing that the following conditions 
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE AS
 * SUBSTITUTED GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <zlib.h>

static off_t offtin(unsigned char *buf)
{
	off_t y;

	y=buf[7]&0x7F;
	y=y*256;y+=buf[6];
	y=y*256;y+=buf[5];
	y=y*256;y+=buf[4];
	y=y*256;y+=buf[3];
	y=y*256;y+=buf[2];
	y=y*256;y+=buf[1];
	y=y*256;y+=buf[0];

	if(buf[7]&0x80) y=-y;

	return y;
}

// ZLIB helper for reading compressed blocks from file
typedef struct {
    FILE* f;
    z_stream strm;
    unsigned char in_buf[16384];
    int eof;
} z_file_reader;

static int z_init(z_file_reader* r, FILE* f, off_t offset) {
    r->f = f;
    fseeko(f, offset, SEEK_SET);
    r->strm.zalloc = Z_NULL;
    r->strm.zfree = Z_NULL;
    r->strm.opaque = Z_NULL;
    r->strm.avail_in = 0;
    r->strm.next_in = Z_NULL;
    r->eof = 0;
    return inflateInit(&r->strm);
}

static ssize_t z_read(z_file_reader* r, unsigned char* out, size_t len) {
    r->strm.avail_out = (uInt)len;
    r->strm.next_out = out;
    
    while (r->strm.avail_out > 0) {
        if (r->strm.avail_in == 0 && !r->eof) {
            r->strm.avail_in = (uInt)fread(r->in_buf, 1, sizeof(r->in_buf), r->f);
            r->strm.next_in = r->in_buf;
            if (r->strm.avail_in < sizeof(r->in_buf)) r->eof = 1;
        }
        
        int ret = inflate(&r->strm, Z_NO_FLUSH);
        if (ret == Z_STREAM_END) break;
        if (ret != Z_OK) return -1;
    }
    return len - r->strm.avail_out;
}

static void z_close(z_file_reader* r) {
    inflateEnd(&r->strm);
}

int bspatch(const char* oldfile, const char* newfile, const char* patchfile)
{
	FILE * f;
	int fd;
	ssize_t oldsize,newsize;
	ssize_t bzctr;
	unsigned char header[32],buf[8];
	unsigned char *old, *new_buf;
	off_t oldpos,newpos;
	off_t ctrl[3];
	off_t i;
    z_file_reader cpf, dpf, epf;

	/* Check arguments */
	if(!oldfile || !newfile || !patchfile) return 1;

	/* Open patch file */
	if ((f = fopen(patchfile, "r")) == NULL) return 1;

	/* Read header */
	if (fread(header, 1, 32, f) < 32) {
		fclose(f);
		return 1;
	}

	/* Check for appropriate magic (Custom magic for ZLIB-BSDIFF) */
	if (memcmp(header, "BSZ40", 5) != 0) {
        fclose(f);
        return 1;
    }

	/* Read lengths from header */
	bzctr=offtin(header+8);
	off_t lenread_header=offtin(header+16);
	newsize=offtin(header+24);
	if((bzctr<0) || (lenread_header<0) || (newsize<0)) {
        fclose(f);
        return 1;
    }

    if (z_init(&cpf, f, 32) != Z_OK) { fclose(f); return 1; }
    
    FILE* f2 = fopen(patchfile, "r");
    if (z_init(&dpf, f2, 32 + bzctr) != Z_OK) { z_close(&cpf); fclose(f); fclose(f2); return 1; }
    
    FILE* f3 = fopen(patchfile, "r");
    if (z_init(&epf, f3, 32 + bzctr + lenread_header) != Z_OK) { z_close(&cpf); z_close(&dpf); fclose(f); fclose(f2); fclose(f3); return 1; }

	if (((fd = open(oldfile, O_RDONLY, 0)) < 0) ||
		((oldsize = lseek(fd, 0, SEEK_END)) < 0) ||
		((old = (unsigned char*)malloc(oldsize + 1)) == NULL) ||
		(lseek(fd, 0, SEEK_SET) != 0) ||
		(read(fd, old, oldsize) != oldsize) ||
		(close(fd) == -1)) return 1;

	if ((new_buf = (unsigned char*)malloc(newsize + 1)) == NULL) return 1;

	oldpos=0;newpos=0;
	while(newpos<newsize) {
		/* Read control data */
		for(i=0;i<=2;i++) {
			if (z_read(&cpf, buf, 8) < 8) return 1;
			ctrl[i]=offtin(buf);
		};

		/* Sanity-check */
		if(newpos+ctrl[0]>newsize) return 1;

		/* Read diff string */
		if (z_read(&dpf, new_buf + newpos, ctrl[0]) < ctrl[0]) return 1;

		/* Add old data to diff string */
		for(i=0;i<ctrl[0];i++)
			if((oldpos+i>=0) && (oldpos+i<oldsize))
				new_buf[newpos+i]+=old[oldpos+i];

		/* Adjust pointers */
		newpos+=ctrl[0];
		oldpos+=ctrl[0];

		/* Sanity-check */
		if(newpos+ctrl[1]>newsize) return 1;

		/* Read extra string */
		if (z_read(&epf, new_buf + newpos, ctrl[1]) < ctrl[1]) return 1;

		/* Adjust pointers */
		newpos+=ctrl[1];
		oldpos+=ctrl[2];
	};

	z_close(&cpf);
    z_close(&dpf);
    z_close(&epf);
    fclose(f);
    fclose(f2);
    fclose(f3);

	/* Write the new file */
	if (((fd = open(newfile, O_CREAT | O_TRUNC | O_WRONLY, 0666)) < 0) ||
		(write(fd, new_buf, newsize) != newsize) ||
		(close(fd) == -1)) return 1;

	free(new_buf);
	free(old);

	return 0;
}
